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

UPDATE pensen.lesson_type
SET code = CASE id
    WHEN 1 THEN 'noLessons'
    WHEN 2 THEN 'continuation'
    WHEN 3 THEN 'start'
    WHEN 4 THEN 'startOptional'
END
WHERE id IN (1, 2, 3, 4);
