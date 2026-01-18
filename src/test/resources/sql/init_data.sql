INSERT INTO public.address (address_id, street_name, building_number, apartment_number, zip_code, city_name, voivode_ship, insert_date, update_date, insert_operator, update_operator) VALUES
(1, 'Zwierzyniecka', '14/16', '18', '60-813', 'Poznań', 'Wielkopolskie', '2024-02-18 23:38:15.781204', null, 'peter.lubowicz@gmail.com', null),
(2, 'Kujawska', '2', '23', '84-835', 'Żory', 'Wielkopolskie', '2024-02-27 22:40:15.092124', null, 'peter.lubowicz@gmail.com', null),
(3, 'Leśna', '2', '45', '60-813', 'Poznań', 'Wielkopolskie', '2024-03-02 13:11:05.528922', null, 'peter.lubowicz@gmail.com', null),
(4, 'Głogowska', '41', '23', '60-813', 'Poznań', 'Wielkopolskie', '2024-03-02 23:16:44.029708', null, 'peter.lubowicz@gmail.com', null),
(5, 'Ogrodowa', '3', '5', '60-813', 'Poznań', 'Wielkopolska', '2024-03-03 00:48:15.179133', null, 'peter.lubowicz@gmail.com', null),
(6, 'Niedzielna', '2', '4', '39-702', 'MysłowiceXD', 'Mazowieckie', '2024-02-27 22:53:49.843817', '2024-03-03 22:23:38.781800', 'peter.lubowicz@gmail.com', 'peter.lubowicz@gmail.com'),
(7, 'Słoneczna', '10', '1', '00-001', 'Warszawa', 'Mazowieckie', '2025-01-10 10:00:00.000000', null, 'system@local', null),
(8, 'Kwiatowa', '5', null, '30-200', 'Kraków', 'Małopolskie', '2025-01-11 11:00:00.000000', null, 'system@local', null),
(9, 'Bałtycka', '12A', '7', '81-000', 'Gdynia', 'Pomorskie', '2025-01-12 12:00:00.000000', null, 'system@local', null);

INSERT INTO public.user_account (user_account_id, first_name, last_name, email, password, role, insert_date, update_date, insert_operator, update_operator, mfa_enabled, secret, hash, hash_date, image, address_id, phone_number, preferred_language) VALUES
    (1, 'Piotr', 'Łubowicz', 'peter.lubowicz@gmail.com', '$2a$10$dBiPZ/Litg7Fgym5YjCYaODXn8eeS2mv21HSpE7zPEdQXRa2xjhD2', 'ADMIN', '2023-11-08 23:08:52.758577', '2024-03-24 18:16:22.794459', 'peter.lubowicz@gmail.com', 'peter.lubowicz@gmail.com', false, null, '50655ab0890412ba512d8c30c38f5c9112ec8503d22fb9241b1e5d53b0ff3f13', '2024-02-25 21:34:32.946390', 'user3-7.jpg', 1, '603050028', 'POLISH');


INSERT INTO public.apartment (apartment_id, apartment_name, leases_number, area, is_rented, user_account_id, insert_date, update_date, insert_operator, update_operator, address_id, image, pinned) VALUES
(1, 'Zwierzyniecka 18', 2, 62.00, true, 1, '2024-02-18 23:38:15.781204', '2024-03-24 19:58:42.275878', 'peter.lubowicz@gmail.com', 'peter.lubowicz@gmail.com', 1, '18-5.JPG', false),
(2, 'Głogowska 41', 3, 90.00, true, 1, '2024-02-18 23:38:15.781204', '2024-03-24 19:58:42.275878', 'peter.lubowicz@gmail.com', 'peter.lubowicz@gmail.com', 4, '18-5.JPG', false),
(3, 'Głogowska 41 - 2', 3, 90.00, true, 1, '2024-02-18 23:38:15.781204', '2024-03-24 19:58:42.275878', 'peter.lubowicz@gmail.com', 'peter.lubowicz@gmail.com', 5, '18-5.JPG', false);


INSERT INTO public.rented_object (rented_object_id, rented_object_name, is_rented, apartment_id, insert_date, update_date, insert_operator, update_operator, last_settlement_date, last_settlement_total_amount) VALUES
(1, 'M18A', true, 1, '2024-02-18 23:38:15.784203', null, 'peter.lubowicz@gmail.com', null, null, null),
(2, 'M18B', true, 1, '2024-02-18 23:38:15.783204', null, 'peter.lubowicz@gmail.com', null, null, null),

(3, 'M23A', true, 2, '2024-02-18 23:38:15.783204', null, 'peter.lubowicz@gmail.com', null, null, null),
(4, 'M23B', true, 2, '2024-02-18 23:38:15.783204', null, 'peter.lubowicz@gmail.com', null, null, null),
(5, 'M23C', true, 2, '2024-02-18 23:38:15.783204', null, 'peter.lubowicz@gmail.com', null, null, null),

(6, 'M23A - 2', true, 3, '2024-02-18 23:38:15.783204', null, 'peter.lubowicz@gmail.com', null, null, null),
(7, 'M23B - 2', true, 3, '2024-02-18 23:38:15.783204', null, 'peter.lubowicz@gmail.com', null, null, null),
(8, 'M23C - 2', true, 3, '2024-02-18 23:38:15.783204', null, 'peter.lubowicz@gmail.com', null, null, null);



--M18A--
INSERT INTO public.meter (meter_id, name, type, rented_object_id, insert_date, update_date, insert_operator, update_operator, meter_number, initial_meter_reading, installation_date, apartment_id, distribute_readings) VALUES
(103, 'woda', 'WATER_COLD', 1, '2024-02-27 22:52:37.761852', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),
(105, 'elektr', 'ELECTRIC', 1, '2024-02-27 22:53:27.569283', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),

--M18B--
(106, 'woda zimna', 'WATER_COLD', 2, '2024-03-02 13:10:14.585596', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),
(107, 'energia elektryczna', 'ELECTRIC', 2, '2024-03-02 13:10:42.263830', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),

--M23A--
(108, 'woda zimna', 'WATER_COLD', 3, '2024-03-02 13:10:14.585596', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),
(109, 'energia elektryczna', 'ELECTRIC', 3, '2024-03-02 13:10:42.263830', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),

--M23B--
(110, 'woda zimna', 'WATER_COLD', 4, '2024-03-02 13:10:14.585596', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),
(111, 'energia elektryczna', 'ELECTRIC', 4, '2024-03-02 13:10:42.263830', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),

--M23C--
(112, 'woda zimna', 'WATER_COLD', 5, '2024-03-02 13:10:14.585596', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),
(113, 'energia elektryczna', 'ELECTRIC', 5, '2024-03-02 13:10:42.263830', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),

--PRĄD KORYTARZ--
(114, 'prąd 23 korytarz', 'ELECTRIC', null, '2025-11-01 20:52:01.824482', '2025-11-01 20:52:14.553632', 'karedon00@gmail.com', 'karedon00@gmail.com', '', null, null, 2, true),

--M23A - 2 --
(115, 'woda zimna', 'WATER_COLD', 6, '2024-03-02 13:10:14.585596', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),
(116, 'energia elektryczna', 'ELECTRIC', 6, '2024-03-02 13:10:42.263830', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),

--M23B - 2 --
(117, 'woda zimna', 'WATER_COLD', 7, '2024-03-02 13:10:14.585596', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),
(118, 'energia elektryczna', 'ELECTRIC', 7, '2024-03-02 13:10:42.263830', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),

--M23C - 2 --
(119, 'woda zimna', 'WATER_COLD', 8, '2024-03-02 13:10:14.585596', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),
(120, 'energia elektryczna', 'ELECTRIC', 8, '2024-03-02 13:10:42.263830', null, 'peter.lubowicz@gmail.com', null, '', null, null, null, false),

--PRĄD KORYTARZ  - 2  --
(121, 'prąd 23 korytarz', 'ELECTRIC', null, '2025-11-01 20:52:01.824482', '2025-11-01 20:52:14.553632', 'karedon00@gmail.com', 'karedon00@gmail.com', '', null, null, 3, true);


INSERT INTO public.meter_reading (meter_reading_id, current_reading, reading_date, consumption, meter_id, insert_date, update_date, insert_operator, update_operator, settled, apartment_settled, settlement_id) VALUES
(1, 50.00000, '2024-02-01 00:00:00.000000', 50.00000, 105, '2024-02-27 22:53:49.852319', null, 'peter.lubowicz@gmail.com', null, false, false, null),
(2, 43.00000, '2024-03-01 00:00:00.000000', 43.00000, 107, '2024-03-02 13:11:05.572152', null, 'peter.lubowicz@gmail.com', null, false, false,null),

(3, 30.00000, '2024-02-01 00:00:00.000000', 30.00000, 109, '2024-02-27 22:53:49.852319', null, 'peter.lubowicz@gmail.com', null, false, false, null),
(4, 30.00000, '2024-03-01 00:00:00.000000', 30.00000, 111, '2024-03-02 13:11:05.572152', null, 'peter.lubowicz@gmail.com', null, false, false,null),
(5, 30.00000, '2024-02-01 00:00:00.000000', 30.00000, 113, '2024-02-27 22:53:49.852319', null, 'peter.lubowicz@gmail.com', null, false, false, null),
(6, 3.00000, '2024-03-01 00:00:00.000000', 3.00000, 114, '2024-03-02 13:11:05.572152', null, 'peter.lubowicz@gmail.com', null, false, false,null),

(7, 97.00000, '2024-02-01 00:00:00.000000', 97.00000, 116, '2024-02-27 22:53:49.852319', null, 'peter.lubowicz@gmail.com', null, false, false, null),
(8, 97.00000, '2024-03-01 00:00:00.000000', 97.00000, 118, '2024-03-02 13:11:05.572152', null, 'peter.lubowicz@gmail.com', null, false, false,null),
(9, 97.00000, '2024-02-01 00:00:00.000000', 97.00000, 120, '2024-02-27 22:53:49.852319', null, 'peter.lubowicz@gmail.com', null, false, false, null),
(10, 6.00000, '2024-03-01 00:00:00.000000', 6.00000, 121, '2024-03-02 13:11:05.572152', null, 'peter.lubowicz@gmail.com', null, false, false,null);


INSERT INTO public.housing_provider (housing_provider_id, name, tax, type, insert_date, update_date, insert_operator, update_operator, user_account_id, conversion_rate) VALUES
(21, 'Orange', null, 'INTERNET', '2024-02-08 20:38:22.372424', null, 'peter.lubowicz@gmail.com', null, 1, null),
(23, 'Enea', 23.00, 'ELECTRICITY', '2024-02-08 20:44:07.679831', null, 'peter.lubowicz@gmail.com', null, 1, null),
(24, 'PGNiG', 23.00, 'GAS', '2024-02-08 20:46:13.399076', '2024-02-09 22:33:09.751144', 'peter.lubowicz@gmail.com', 'peter.lubowicz@gmail.com', 1, 11.44200),
(25, 'Sobótka woda', null, 'WATER', '2024-02-11 18:30:05.170915', null, 'peter.lubowicz@gmail.com', null, 1, null),
(26, 'Vectra', null, 'INTERNET', '2024-02-16 16:23:59.887456', null, 'peter.lubowicz@gmail.com', null, 1, null),
(27, 'Enea - 2', 23.00, 'ELECTRICITY', '2024-02-08 20:44:07.679831', null, 'peter.lubowicz@gmail.com', null, 1, null);


--ORANGE INTERNET--
INSERT INTO public.provider_field (provider_field_id, name, price, billing_method, housing_provider_id, insert_date, update_date, insert_operator, update_operator)
VALUES (1, '', 125.00000, 'MONTHLY', 21, '2024-02-08 20:38:22.376428', null, 'peter.lubowicz@gmail.com', null),

--ENEA PRĄD--
       (53, 'Energia elektryczna czynna', 0.5050, 'ACCORDING_TO_CONSUMPTION', 23, '2024-02-08 20:44:07.680841', null, 'peter.lubowicz@gmail.com', null),
       (54, 'Opłata stała sieciowa - układ 1-fazowy', 7.2500, 'MONTHLY', 23, '2024-02-08 20:44:07.680841', null, 'peter.lubowicz@gmail.com', null),
       (56, 'Opłata przejściowa > 1200 kWh', 0.33000, 'MONTHLY', 23, '2024-02-08 20:44:07.681846', null, 'peter.lubowicz@gmail.com', null),
       (58, 'Opłata mocowa', 11.44000, 'MONTHLY', 23, '2024-02-08 20:44:07.683247', null, 'peter.lubowicz@gmail.com', null),
       (55, 'Opłata zmienna sieciowa', 0.2456, 'ACCORDING_TO_CONSUMPTION', 23, '2024-02-08 20:44:07.681846', null, 'peter.lubowicz@gmail.com', null),
       (60, 'Opłata jakościowa', 0.0321, 'ACCORDING_TO_CONSUMPTION', 23, '2024-02-08 20:44:07.684299', null, 'peter.lubowicz@gmail.com', null),
       (57, 'Opłata OZE', 0.0035, 'ACCORDING_TO_CONSUMPTION', 23, '2024-02-08 20:44:07.681846', null, 'peter.lubowicz@gmail.com', null),
       (59, 'Opłata kogeneracyjna', 0.0030, 'ACCORDING_TO_CONSUMPTION', 23, '2024-02-08 20:44:07.683247', null, 'peter.lubowicz@gmail.com', null),
       (61, 'Opłata abonamentowa', 3.84000, 'MONTHLY', 23, '2024-02-08 20:44:07.684299', null, 'peter.lubowicz@gmail.com', null),

--ENEA PRĄD - 2 --
        (68, 'Energia czynna całodobowa', 0.50500, 'ACCORDING_TO_CONSUMPTION', 27, '2025-10-25 20:41:04.528116', null, 'karedon00@gmail.com', null),
        (69, 'Opłata stała sieciowa - układ 3-fazowy', 10.14000, 'MONTHLY', 27, '2025-10-25 20:41:04.528116', null, 'karedon00@gmail.com', null),
        (70, 'Opłata zmienna sieciowa', 0.24560, 'ACCORDING_TO_CONSUMPTION', 27, '2025-10-25 20:41:04.529189', null, 'karedon00@gmail.com', null),
        (71, 'Opłata jakościowa', 0.03210, 'ACCORDING_TO_CONSUMPTION', 27, '2025-10-25 20:41:04.529189', null, 'karedon00@gmail.com', null),
        (72, 'Opłata OZE', 0.00350, 'ACCORDING_TO_CONSUMPTION', 27, '2025-10-25 20:41:04.529189', null, 'karedon00@gmail.com', null),
        (73, 'Opłata kogeneracyjna', 0.00300, 'ACCORDING_TO_CONSUMPTION', 27, '2025-10-25 20:41:04.530189', null, 'karedon00@gmail.com', null),
        (74, 'Opłata abonamentowa', 3.84000, 'MONTHLY', 27, '2025-10-25 20:41:04.530189', null, 'karedon00@gmail.com', null),
        (75, 'Opłata przejściowa 500 - 1200 kWh', 0.33000, 'MONTHLY', 27, '2025-10-25 20:41:04.528116', '2025-11-02 12:52:18.382886', 'karedon00@gmail.com', 'karedon00@gmail.com'),

--PGNiG GAZ--
       (62, 'Opłata abonamentowa', 8.67000, 'MONTHLY', 24, '2024-02-08 20:46:13.400076', null, 'peter.lubowicz@gmail.com', null),
       (63, 'Paliwo gazowe', 0.20017, 'ACCORDING_TO_CONSUMPTION', 24, '2024-02-08 20:46:13.400076', null, 'peter.lubowicz@gmail.com', null),
       (64, 'Dystrybucyjna stała', 9.59000, 'MONTHLY', 24, '2024-02-08 20:46:13.401080', null, 'peter.lubowicz@gmail.com', null),
       (65, 'Dystrybucyjna zmienna', 0.03620, 'ACCORDING_TO_CONSUMPTION', 24, '2024-02-08 20:46:13.401080', null, 'peter.lubowicz@gmail.com', null),

--SOBÓTKA WODA--
       (66, '', 14.83000, 'MONTHLY', 25, '2024-02-11 18:30:05.174953', null, 'peter.lubowicz@gmail.com', null),

--VECTRA INTERNET--
       (67, '', 130.00000, 'MONTHLY', 26, '2024-02-16 16:23:59.890504', null, 'peter.lubowicz@gmail.com', null);

INSERT INTO public.apartment_housing_provider (apartment_housing_provider_id, apartment_id, housing_provider_id) VALUES
(1, 1, 21), --Zwierzyniecka 18 - Orange
(2, 1, 23), --Zwierzyniecka 18 - Enea
(3, 1, 25), --Zwierzyniecka 18 - Sobótka woda

(4, 2, 21), -- Głogowska 23 - Orange
(5, 2, 23), --Głogowska 23 - Enea
(6, 2, 25), --Głogowska 23 - Sobótka woda

(7, 3, 21), --Głogowska 23 - 2 - Orange
(8, 3, 27), --Głogowska 23 - 2 - Enea - 2
(9, 3, 25); --Głogowska 23 - 2 - Sobótka woda


INSERT INTO public.lease_agreement (lease_agreement_id, start_contract_date, end_contract_date, deposit, deposit_paid, payment_due_day_of_month, rent_amount, compensation_amount, internet_fee, gas_deposit, initial_energy_meter_reading, initial_water_meter_reading, initial_gas_meter_reading, deposit_return_date, returned_deposit_amount, is_contract_active, insert_date, update_date, insert_operator, update_operator, contract_signing_date) VALUES
(102, '2024-03-01 00:00:00.000000', '2024-12-31 00:00:00.000000', 1000.00, 1000.00, 10, 1100.00, 2450.00, 89.00, null, 0.00000, 10.00000, null, null, null, true, '2024-03-02 23:31:25.843837', null, 'peter.lubowicz@gmail.com', null, '2024-03-01 00:00:00.000000'),
(103, '2024-01-01 00:00:00.000000', '2024-07-31 00:00:00.000000', 2000.00, 2000.00, 10, 800.00, 2000.00, 85.00, null, 0.00000, 15.00000, null, null, null, true, '2024-03-03 00:48:15.184088', null, 'peter.lubowicz@gmail.com', null, '2024-03-01 00:00:00.000000'),
(104, '2024-01-01 00:00:00.000000', '2024-07-31 00:00:00.000000', 2000.00, 2000.00, 10, 800.00, 2000.00, 85.00, null, 0.00000, 15.00000, null, null, null, true, '2024-03-03 00:48:15.184088', null, 'peter.lubowicz@gmail.com', null, '2024-03-01 00:00:00.000000'),
(105, '2024-01-01 00:00:00.000000', '2024-07-31 00:00:00.000000', 2000.00, 2000.00, 10, 800.00, 2000.00, 85.00, null, 0.00000, 15.00000, null, null, null, true, '2024-03-03 00:48:15.184088', null, 'peter.lubowicz@gmail.com', null, '2024-03-01 00:00:00.000000'),
(106, '2024-01-01 00:00:00.000000', '2024-07-31 00:00:00.000000', 2000.00, 2000.00, 10, 800.00, 2000.00, 85.00, null, 0.00000, 15.00000, null, null, null, true, '2024-03-03 00:48:15.184088', null, 'peter.lubowicz@gmail.com', null, '2024-03-01 00:00:00.000000'),

(107, '2024-01-01 00:00:00.000000', '2024-07-31 00:00:00.000000', 2000.00, 2000.00, 10, 800.00, 2000.00, 85.00, null, 0.00000, 15.00000, null, null, null, true, '2024-03-03 00:48:15.184088', null, 'peter.lubowicz@gmail.com', null, '2024-03-01 00:00:00.000000'),
(108, '2024-01-01 00:00:00.000000', '2024-07-31 00:00:00.000000', 2000.00, 2000.00, 10, 800.00, 2000.00, 85.00, null, 0.00000, 15.00000, null, null, null, true, '2024-03-03 00:48:15.184088', null, 'peter.lubowicz@gmail.com', null, '2024-03-01 00:00:00.000000'),
(109, '2024-01-01 00:00:00.000000', '2024-07-31 00:00:00.000000', 2000.00, 2000.00, 10, 800.00, 2000.00, 85.00, null, 0.00000, 15.00000, null, null, null, true, '2024-03-03 00:48:15.184088', null, 'peter.lubowicz@gmail.com', null, '2024-03-01 00:00:00.000000');



INSERT INTO public.tenant (tenant_id, first_name, last_name, account_number, phone_number, address_id, lease_agreement_id, insert_date, update_date, insert_operator, update_operator, email, rented_object_id) VALUES
(112, 'Marta', 'Duda', '', '', 2, 102, '2024-03-02 23:31:25.839833', null, 'peter.lubowicz@gmail.com', null, 'marta.duda@gmail.com', 1),
(113, 'Tymon', 'Czajka', '', '', 3, 103, '2024-03-03 00:48:15.176091', null, 'peter.lubowicz@gmail.com', null, 'tymon.czajka@gmail.com', 2),
(114, 'Bogdan', 'Bonar', '', '', 4, 104, '2024-03-03 00:48:15.176091', null, 'peter.lubowicz@gmail.com', null, 'bogdan.boner@gmail.com', 3),
(115, 'Domino', 'Jachaś', '', '', 5, 105, '2024-03-03 00:48:15.176091', null, 'peter.lubowicz@gmail.com', null, 'domino.jachas@gmail.com', 4),
(116, 'Eris', 'Butkiewicz', '', '', 6, 106, '2024-03-03 00:48:15.176091', null, 'peter.lubowicz@gmail.com', null, 'eris.butkiewicz@gmail.com', 5),

(117, 'Enzo', 'Butkiewicz', '', '', 7, 107, '2024-03-03 00:48:15.176091', null, 'peter.lubowicz@gmail.com', null, 'bogdan.boner@gmail.com', 6),
(118, 'Logan', 'Nerko', '', '', 8, 108, '2024-03-03 00:48:15.176091', null, 'peter.lubowicz@gmail.com', null, 'domino.jachas@gmail.com', 7),
(119, 'Gosia', 'Butkiewicz', '', '', 9, 109, '2024-03-03 00:48:15.176091', null, 'peter.lubowicz@gmail.com', null, 'eris.butkiewicz@gmail.com', 8);
