/*
 * Copyright (C) 2022 by Sebastian Forster, Stefan Rothe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY); without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.kinet.pensen.server;

import ch.kinet.JsonArray;
import ch.kinet.JsonObject;
import ch.kinet.jjwt.SigningKeyProvider;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class MicrosoftKeys implements SigningKeyProvider {

    private final String tenant;
    private Map<String, PublicKey> keys;

    public static MicrosoftKeys create(String tenant) {
        return new MicrosoftKeys(tenant);
    }

    public MicrosoftKeys(String tenant) {
        keys = new HashMap<>();
        this.tenant = tenant;
    }

    @Override
    public PublicKey getSigningKey(String keyId) {
        if (!keys.containsKey(keyId)) {
            update();
        }

        if (!keys.containsKey(keyId)) {
            return null;
        }

        return keys.get(keyId);
    }

    private boolean update() {
        Map<String, PublicKey> newKeys = new HashMap<>();
        String url = "https://login.microsoftonline.com/" + tenant + "/discovery/v2.0/keys";
        JsonObject response = JsonObject.createFromUrl(url);
        if (response == null) {
            return false;
        }

        JsonArray array = response.getArray("keys");
        if (array == null) {
            return false;
        }

        for (int i = 0; i < array.length(); i++) {
            JsonObject object = array.getObject(i);
            String keyId = object.getString("kid");
            String modulus = object.getString("n");
            String exponent = object.getString("e");
            PublicKey key = createPublicKey(modulus, exponent);
            if (key == null) {
                return false;
            }

            newKeys.put(keyId, key);
        }

        if (newKeys.isEmpty()) {
            return false;
        }

        this.keys = newKeys;
        return true;
    }

    /**
     * Converts a Base64 encoded number to a Java BigInteger.
     *
     * @param base64 the Base64 encoded number
     * @return the number as BigInteger
     */
    private static BigInteger base64ToBigInt(String base64) {
        byte[] bytes = Base64.getUrlDecoder().decode(base64);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return new BigInteger(sb.toString(), 16);
    }

    private static PublicKey createPublicKey(String encodedModulus, String encodedExponent) {
        try {
            BigInteger modulus = base64ToBigInt(encodedModulus);
            BigInteger exponent = base64ToBigInt(encodedExponent);
            KeyFactory rsa = KeyFactory.getInstance("RSA");
            return rsa.generatePublic(new RSAPublicKeySpec(modulus, exponent));

        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            return null;
        }
    }
}
