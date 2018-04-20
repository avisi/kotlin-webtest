CREATE TABLE cars (
  name    VARCHAR(150),
  price   INT,
  comment VARCHAR(255) NULL,
  tijd    TIMESTAMP    NULL,
  tijd2   TIMESTAMP    NULL
);

INSERT INTO cars (name, price) VALUES ('Audi', 52642);
INSERT INTO cars (name, price, comment) VALUES ('Mercedes', 57127, 'duur');
INSERT INTO cars (name, price, tijd, tijd2) VALUES ('Skoda', 9000, '2013-10-21 00:00:10', '2013-10-21 00:00:15');
INSERT INTO cars (name, price) VALUES ('Volvo', 29000);
INSERT INTO cars (name, price, comment) VALUES ('Bentley', 350000, 'duur');
INSERT INTO cars (name, price) VALUES ('Citroen', 21000);
INSERT INTO cars (name, price) VALUES ('Hummer', 41400);
INSERT INTO cars (name, price) VALUES ('Volkswagen', 21600);