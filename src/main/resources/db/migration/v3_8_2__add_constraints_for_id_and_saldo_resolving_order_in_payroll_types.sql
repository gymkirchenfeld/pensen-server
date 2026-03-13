ALTER TABLE pensen.payroll_type
ADD CONSTRAINT id_max CHECK (id < 100000),
ADD CONSTRAINT saldo_resolving_order_max CHECK (saldo_resolving_order < 100000)