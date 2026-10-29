ALTER TABLE pensen.authorisation RENAME TO pensen.account;

ALTER TABLE pensen.account RENAME COLUMN account_name TO name;

UPDATE pensen.calculation_mode SET code = 'percent' WHERE id = 1;
UPDATE pensen.calculation_mode SET code = 'lessons' WHERE id = 2;

INSERT INTO pensen.calculation_mode (id, code, description)
VALUES (99, 'percentAgeReliefIncluded', 'Prozent, AE inkl.');

INSERT INTO pensen.calculation_mode (id, code, description)
VALUES (100, 'lessonsAgeReliefIncluded', 'Lektionen (SAP), AE inkl.');

INSERT INTO pensen.calculation_mode (id, code, description)
VALUES (101, 'lessons2AgeReliefIncluded', 'Lektionen (SAP) 2, AE inkl.');
