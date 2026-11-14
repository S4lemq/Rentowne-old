-- Czyszczenie danych przed każdym testem
DELETE FROM meter_reading;
DELETE FROM settlement;
DELETE FROM invoice;
DELETE FROM tenant;
DELETE FROM lease_agreement;
DELETE FROM apartment_housing_provider;
DELETE FROM provider_field;
DELETE FROM housing_provider;
DELETE FROM meter;
DELETE FROM rented_object;
DELETE FROM apartment;
DELETE FROM user_account;
DELETE FROM address;

-- Resetowanie sekwencji
ALTER SEQUENCE settlement_seq RESTART WITH 1;
ALTER SEQUENCE address_seq RESTART WITH 1;
ALTER SEQUENCE user_account_seq RESTART WITH 1;
ALTER SEQUENCE apartment_seq RESTART WITH 1;
ALTER SEQUENCE rented_object_seq RESTART WITH 1;
ALTER SEQUENCE meter_seq RESTART WITH 1;
ALTER SEQUENCE meter_reading_seq RESTART WITH 1;
ALTER SEQUENCE housing_provider_seq RESTART WITH 1;
ALTER SEQUENCE provider_field_seq RESTART WITH 1;
ALTER SEQUENCE apartment_housing_provider_seq RESTART WITH 1;
ALTER SEQUENCE lease_agreement_seq RESTART WITH 1;
ALTER SEQUENCE tenant_seq RESTART WITH 1;
ALTER SEQUENCE invoice_seq RESTART WITH 1;
