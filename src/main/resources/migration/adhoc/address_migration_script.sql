-- You may need to run these if running a second time
-- TRUNCATE TABLE address RESTART IDENTITY CASCADE;
-- TRUNCATE TABLE licence_appointment_address RESTART IDENTITY CASCADE;
--
-- This SQL tries to make sense out of the comma delimited string addresses found in the licence
-- Info on Lateral joins
--    You use a LATERAL JOIN when:
--    The right-hand side of the join needs to refer to columns from the left-hand side.
--    You want to call a subquery, function, or derived table for each row in the outer query.

-- Stage 1 will :
--  1. Clean and extract post codes
--  2. Remove duplicate elements in the string
--  3. Ignore null addresses and

CREATE
TEMP TABLE tmp_stage_1 AS
SELECT l.id,
	   address_cleaned.value AS appointment_address,
	   array_length(address_array.value,1) AS address_part_length,
	   UPPER(CASE
				 WHEN length(full_postcode) > 5 THEN SUBSTRING(cleaned FROM 1 FOR length(cleaned) - 3) || ' ' ||
													 SUBSTRING(cleaned FROM length(cleaned) - 2 FOR 3)
				 ELSE coalesce(full_postcode,left_over_1,left_over_2,left_over_3) end
	   ) AS postcode
FROM licence l
		 LEFT JOIN LATERAL (
	-- removes duplicate elements
	SELECT regexp_replace(regexp_replace(string_agg(value, ',' ORDER BY ord), ',{2,}', ',', 'g'), '^,|,$', '', 'g') AS VALUE
FROM (
	SELECT DISTINCT ON (TRIM (VALUE)) TRIM (VALUE) AS VALUE, ord FROM unnest(string_to_array(l.appointment_address, ',')) WITH ORDINALITY AS t(VALUE, ord) ) ) address_cleaned
ON TRUE
	LEFT JOIN LATERAL ( SELECT string_to_array(address_cleaned.value, ',') AS VALUE ) address_array ON TRUE
	-- 👇 Extract full postcode (with or without space), clean, and standardize
	LEFT JOIN LATERAL (
	SELECT
	(regexp_matches( regexp_replace(address_cleaned.value, '[^A-Za-z0-9 ,]', '', 'g'), '(([A-PR-UWYZ][0-9][0-9]?|[A-PR-UWYZ][A-HK-Y][0-9][0-9]?|[A-PR-UWYZ][0-9][A-HJKPSTUW]|[A-PR-UWYZ][A-HK-Y][0-9][ABEHMNPRVWXY])\s*[0-9][ABD-HJLNP-UW-Z]{2})', 'i' ))[1]
	AS full_postcode
	) best_match ON true
	LEFT JOIN LATERAL (
	SELECT 	(regexp_matches(TRIM(address_array.value[array_length(address_array.value, 1)]), '^(?=[A-Za-z][A-Za-z0-9 ]{2,7}$)(?=.*\d)[A-Za-z0-9 ]{3,8}$'))[1] AS left_over_1,
	(regexp_matches(TRIM(address_array.value[array_length(address_array.value, 1)-1]), '^(?=[A-Za-z][A-Za-z0-9 ]{2,7}$)(?=.*\d)[A-Za-z0-9 ]{3,8}$'))[1] AS left_over_2,
	(regexp_matches(TRIM(address_array.value[array_length(address_array.value, 1)-2]), '^(?=[A-Za-z][A-Za-z0-9 ]{2,7}$)(?=.*\d)[A-Za-z0-9 ]{3,8}$'))[1] AS left_over_3
	where  full_postcode is null
	) sub ON true
	CROSS JOIN LATERAL ( SELECT regexp_replace(COALESCE (full_postcode, ''), '[^A-Za-z0-9]', '', 'g') AS cleaned )
WHERE l.appointment_address IS NOT NULL;

-- A Simple table to allow us to determine the Country by 2 digit post code prefix
CREATE
TEMP TABLE tmp_postcode_country AS
SELECT *
FROM (
		 VALUES
			 -- CROSS BOARDER POST CODES
			 ('SY',NULL),
			 ('LD',NULL),
			 ('HR',NULL),
			 ('CH',NULL),
			 ('TD',NULL),
			 ('CA',NULL),
			 -- WALES-only
			 ('CF','WALES'),
			 ('LL','WALES'),
			 ('NP','WALES'),
			 ('SA','WALES'),
			 -- NORTHEN_IRELAND-only
			 ('BT','NORTHEN_IRELAND'),
			 -- ENGLAND-only
			 ('AL','ENGLAND'),
			 ('B','ENGLAND'),
			 ('BA','ENGLAND'),
			 ('BB','ENGLAND'),
			 ('BD','ENGLAND'),
			 ('BH','ENGLAND'),
			 ('BL','ENGLAND'),
			 ('BN','ENGLAND'),
			 ('BR','ENGLAND'),
			 ('BS','ENGLAND'),
			 ('CB','ENGLAND'),
			 ('CM','ENGLAND'),
			 ('CO','ENGLAND'),
			 ('CR','ENGLAND'),
			 ('CT','ENGLAND'),
			 ('CV','ENGLAND'),
			 ('CW','ENGLAND'),
			 ('DA','ENGLAND'),
			 ('DE','ENGLAND'),
			 ('DH','ENGLAND'),
			 ('DL','ENGLAND'),
			 ('DN','ENGLAND'),
			 ('DT','ENGLAND'),
			 ('DY','ENGLAND'),
			 ('E','ENGLAND'),
			 ('EC','ENGLAND'),
			 ('EN','ENGLAND'),
			 ('EX','ENGLAND'),
			 ('FY','ENGLAND'),
			 ('GL','ENGLAND'),
			 ('GU','ENGLAND'),
			 ('HA','ENGLAND'),
			 ('HD','ENGLAND'),
			 ('HG','ENGLAND'),
			 ('HP','ENGLAND'),
			 ('HU','ENGLAND'),
			 ('HX','ENGLAND'),
			 ('IG','ENGLAND'),
			 ('IP','ENGLAND'),
			 ('KT','ENGLAND'),
			 ('L','ENGLAND'),
			 ('LA','ENGLAND'),
			 ('LE','ENGLAND'),
			 ('LN','ENGLAND'),
			 ('LS','ENGLAND'),
			 ('LU','ENGLAND'),
			 ('M','ENGLAND'),
			 ('ME','ENGLAND'),
			 ('MK','ENGLAND'),
			 ('N','ENGLAND'),
			 ('NE','ENGLAND'),
			 ('NG','ENGLAND'),
			 ('NN','ENGLAND'),
			 ('NR','ENGLAND'),
			 ('NW','ENGLAND'),
			 ('OL','ENGLAND'),
			 ('OX','ENGLAND'),
			 ('PE','ENGLAND'),
			 ('PL','ENGLAND'),
			 ('PO','ENGLAND'),
			 ('PR','ENGLAND'),
			 ('RG','ENGLAND'),
			 ('RH','ENGLAND'),
			 ('RM','ENGLAND'),
			 ('S','ENGLAND'),
			 ('SE','ENGLAND'),
			 ('SG','ENGLAND'),
			 ('SK','ENGLAND'),
			 ('SL','ENGLAND'),
			 ('SM','ENGLAND'),
			 ('SN','ENGLAND'),
			 ('SO','ENGLAND'),
			 ('SP','ENGLAND'),
			 ('SR','ENGLAND'),
			 ('SS','ENGLAND'),
			 ('ST','ENGLAND'),
			 ('SW','ENGLAND'),
			 ('TA','ENGLAND'),
			 ('TF','ENGLAND'),
			 ('TN','ENGLAND'),
			 ('TQ','ENGLAND'),
			 ('TR','ENGLAND'),
			 ('TS','ENGLAND'),
			 ('TW','ENGLAND'),
			 ('UB','ENGLAND'),
			 ('W','ENGLAND'),
			 ('WA','ENGLAND'),
			 ('WC','ENGLAND'),
			 ('WD','ENGLAND'),
			 ('WF','ENGLAND'),
			 ('WN','ENGLAND'),
			 ('WR','ENGLAND'),
			 ('WS','ENGLAND'),
			 ('WV','ENGLAND'),
			 ('YO','ENGLAND'),
			 -- SCOTLAND-only
			 ('AB','SCOTLAND'),
			 ('DD','SCOTLAND'),
			 ('DG','SCOTLAND'),
			 ('EH','SCOTLAND'),
			 ('FK','SCOTLAND'),
			 ('G','SCOTLAND'),
			 ('HS','SCOTLAND'),
			 ('IV','SCOTLAND'),
			 ('KA','SCOTLAND'),
			 ('KW','SCOTLAND'),
			 ('KY','SCOTLAND'),
			 ('ML','SCOTLAND'),
			 ('PA','SCOTLAND'),
			 ('PH','SCOTLAND'),
			 ('ZE','SCOTLAND')) AS t(postcode_prefix,country);


-- Stage 2 Adds post code prefix and country based on post code

CREATE
TEMP TABLE tmp_stage_2 AS
SELECT address.*, country.postcode_prefix, country.country
FROM tmp_stage_1 address
		 LEFT JOIN tmp_postcode_country AS country ON POSITION(country.postcode_prefix IN address.postcode) = 1
WHERE LENGTH(country.postcode_prefix) = 2;
-- The above looks at post codes with a prefix of two SA,CF for SA42..

-- delete processed data from last source table
DELETE
FROM tmp_stage_1 USING tmp_stage_2
WHERE tmp_stage_1.id = tmp_stage_2.id;


INSERT INTO tmp_stage_2 (id,appointment_address,address_part_length,postcode,postcode_prefix,country)
SELECT address.*, country.postcode_prefix, country.country
FROM tmp_stage_1 address
		 LEFT JOIN tmp_postcode_country AS country ON POSITION(country.postcode_prefix IN address.postcode) = 1
WHERE LENGTH(country.postcode_prefix) = 1;
-- The above looks at post codes with a prefix of one S,C for S12

DELETE
FROM tmp_stage_1 USING tmp_stage_2
WHERE tmp_stage_1.id = tmp_stage_2.id;

-- adds un processed data
INSERT INTO tmp_stage_2 (id,appointment_address,address_part_length,postcode,postcode_prefix,country)
SELECT address.*, NULL, NULL
FROM tmp_stage_1 address;

DELETE
FROM tmp_stage_1 USING tmp_stage_2
WHERE tmp_stage_1.id = tmp_stage_2.id;

-- Creates table that link county to country

CREATE
TEMP TABLE tmp_uk_counties (
    country TEXT NOT NULL,
    county TEXT NOT NULL
);

-- Inserts:
INSERT INTO tmp_uk_counties (country,county)
VALUES ('ENGLAND','Kent'),
	   ('ENGLAND','Cheshire'),
	   ('ENGLAND','Durham'),
	   ('ENGLAND','Surrey'),
	   ('ENGLAND','Cornwall'),
	   ('ENGLAND','Yorkshire'),
	   ('ENGLAND','County Durham'),
	   ('ENGLAND','Tyne and Wear'),
	   ('ENGLAND','Middlesex'),
	   ('ENGLAND','Worcestershire'),
	   ('ENGLAND','City of London'),
	   ('ENGLAND','Lancashire'),
	   ('ENGLAND','Sussex'),
	   ('ENGLAND','South Yorkshire'),
	   ('ENGLAND','Norfolk'),
	   ('ENGLAND','Herefordshire'),
	   ('ENGLAND','East Riding of Yorkshire'),
	   ('ENGLAND','East Yorkshire'), -- offically know as above
	   ('ENGLAND','Hertfordshire'),
	   ('ENGLAND','Lincolnshire'),
	   ('ENGLAND','West Yorkshire'),
	   ('ENGLAND','Warwickshire'),
	   ('ENGLAND','Staffordshire'),
	   ('ENGLAND','Somerset'),
	   ('ENGLAND','Bristol'),
	   ('ENGLAND','Cumbria'),
	   ('ENGLAND','Hampshire'),
	   ('ENGLAND','Northamptonshire'),
	   ('ENGLAND','Westmorland'),
	   ('ENGLAND','Greater Manchester'),
	   ('ENGLAND','Devon'),
	   ('ENGLAND','Merseyside'),
	   ('ENGLAND','Cambridgeshire'),
	   ('ENGLAND','Gloucestershire'),
	   ('ENGLAND','Essex'),
	   ('ENGLAND','Huntingdonshire'),
	   ('ENGLAND','Dorset'),
	   ('ENGLAND','Northumberland'),
	   ('ENGLAND','Isle of Wight'),
	   ('ENGLAND','Greater London'),
	   ('ENGLAND','Buckinghamshire'),
	   ('ENGLAND','Rutland'),
	   ('ENGLAND','Berkshire'),
	   ('ENGLAND','Leicestershire'),
	   ('ENGLAND','East Sussex'),
	   ('ENGLAND','Suffolk'),
	   ('ENGLAND','Wiltshire'),
	   ('ENGLAND','West Sussex'),
	   ('ENGLAND','Derbyshire'),
	   ('ENGLAND','Bedfordshire'),
	   ('ENGLAND','North Yorkshire'),
	   ('ENGLAND','Oxfordshire'),
	   ('ENGLAND','Nottinghamshire'),
	   ('ENGLAND','Shropshire'),
	   ('ENGLAND','West Midlands'),
	   ('ENGLAND','Cumberland'),
	   ('NORTHEN_IRELAND','Lisburn and Castlereagh'),
	   ('NORTHEN_IRELAND','Derry and Strabane'),
	   ('NORTHEN_IRELAND','Belfast'),
	   ('NORTHEN_IRELAND','Tyrone'),
	   ('NORTHEN_IRELAND','Londonderry'),
	   ('NORTHEN_IRELAND','Armagh'),
	   ('NORTHEN_IRELAND','Antrim and Newtownabbey'),
	   ('NORTHEN_IRELAND','Fermanagh and Omagh'),
	   ('NORTHEN_IRELAND','Down'),
	   ('NORTHEN_IRELAND','Armagh, Banbridge and Craigavon'),
	   ('NORTHEN_IRELAND','Fermanagh'),
	   ('NORTHEN_IRELAND','Causeway Coast and Glens'),
	   ('NORTHEN_IRELAND','Antrim'),
	   ('NORTHEN_IRELAND','Newry, Mourne and Down'),
	   ('NORTHEN_IRELAND','Mid Ulster'),
	   ('NORTHEN_IRELAND','Mid and East Antrim'),
	   ('SCOTLAND','Kirkcudbrightshire'),
	   ('SCOTLAND','Caithness'),
	   ('SCOTLAND','Ross-shire'),
	   ('SCOTLAND','Kincardineshire'),
	   ('SCOTLAND','Dumfriesshire'),
	   ('SCOTLAND','Orkney Islands'),
	   ('SCOTLAND','Fife'),
	   ('SCOTLAND','Peeblesshire'),
	   ('SCOTLAND','West Lothian'),
	   ('SCOTLAND','Sutherland'),
	   ('SCOTLAND','Dunbartonshire'),
	   ('SCOTLAND','East Ayrshire'),
	   ('SCOTLAND','Argyll and Bute'),
	   ('SCOTLAND','Banffshire'),
	   ('SCOTLAND','South Lanarkshire'),
	   ('SCOTLAND','Roxburghshire'),
	   ('SCOTLAND','Lanarkshire'),
	   ('SCOTLAND','Moray'),
	   ('SCOTLAND','Glasgow City'),
	   ('SCOTLAND','Nairnshire'),
	   ('SCOTLAND','Inverness-shire'),
	   ('SCOTLAND','Clackmannanshire'),
	   ('SCOTLAND','Stirling'),
	   ('SCOTLAND','Dumfries and Galloway'),
	   ('SCOTLAND','Aberdeen City'),
	   ('SCOTLAND','Perthshire'),
	   ('SCOTLAND','East Dunbartonshire'),
	   ('SCOTLAND','North Lanarkshire'),
	   ('SCOTLAND','Wigtownshire'),
	   ('SCOTLAND','Angus'),
	   ('SCOTLAND','Inverclyde'),
	   ('SCOTLAND','Midlothian'),
	   ('SCOTLAND','West Dunbartonshire'),
	   ('SCOTLAND','Argyll'),
	   ('SCOTLAND','Kinross-shire'),
	   ('SCOTLAND','Buteshire'),
	   ('SCOTLAND','Falkirk'),
	   ('SCOTLAND','East Renfrewshire'),
	   ('SCOTLAND','Perth and Kinross'),
	   ('SCOTLAND','Aberdeenshire'),
	   ('SCOTLAND','East Lothian'),
	   ('SCOTLAND','Ayrshire'),
	   ('SCOTLAND','Western Isles'),
	   ('SCOTLAND','South Ayrshire'),
	   ('SCOTLAND','Highland'),
	   ('SCOTLAND','Shetland Islands'),
	   ('SCOTLAND','Orkney'),
	   ('SCOTLAND','Shetland'),
	   ('SCOTLAND','Berwickshire'),
	   ('SCOTLAND','Scottish Borders'),
	   ('SCOTLAND','Dundee City'),
	   ('SCOTLAND','Selkirkshire'),
	   ('SCOTLAND','City of Edinburgh'),
	   ('SCOTLAND','Renfrewshire'),
	   ('SCOTLAND','Stirlingshire'),
	   ('SCOTLAND','North Ayrshire'),
	   ('WALES','Powys'),
	   ('WALES','Glamorgan'),
	   ('WALES','Denbighshire'),
	   ('WALES','Cardiff'),
	   ('WALES','Carmarthenshire'),
	   ('WALES','Conwy'),
	   ('WALES','Flintshire'),
	   ('WALES','Swansea'),
	   ('WALES','Gwent'),
	   ('WALES','Anglesey'),
	   ('WALES','Dyfed'),
	   ('WALES','Merionethshire'),
	   ('WALES','Cardiganshire'),
	   ('WALES','South Glamorgan'),
	   ('WALES','Mid Glamorgan'),
	   ('WALES','Clwyd'),
	   ('WALES','Radnorshire'),
	   ('WALES','Monmouthshire'),
	   ('WALES','West Glamorgan'),
	   ('WALES','Caernarfonshire'),
	   ('WALES','Vale of Glamorgan'),
	   ('WALES','Brecknockshire'),
	   ('WALES','Gwynedd'),
	   ('WALES','Montgomeryshire'),
	   ('WALES','Pembrokeshire'),
	   ('WALES','Morgannwg'),
	   ('WALES','Sir Ddinbych'),
	   ('WALES','Caerdydd'),
	   ('WALES','Sir Gaerfyrddin'),
	   ('WALES','Sir y Fflint'),
	   ('WALES','Abertawe'),
	   ('WALES','Ynys Môn'),
	   ('WALES','Meirionnydd'),
	   ('WALES','Ceredigion'),
	   ('WALES','De Morgannwg'),
	   ('WALES','Morgannwg Ganol'),
	   ('WALES','Sir Faesyfed'),
	   ('WALES','Sir Fynwy'),
	   ('WALES','Gorllewin Morgannwg'),
	   ('WALES','Sir Gaernarfon'),
	   ('WALES','Bro Morgannwg'),
	   ('WALES','Sir Frycheiniog'),
	   ('WALES','Sir Drefaldwyn'),
	   ('WALES','Sir Benfro');

-- Stage 3 get's county and country (if not all ready aquuired)

CREATE
TEMP TABLE tmp_stage_3 AS
SELECT id,
	   appointment_address,
	   address_part_length,
	   postcode,
	   postcode_prefix,
	   counties.county,
	   COALESCE(l.country,counties.country) AS country
FROM tmp_stage_2 l
		 LEFT JOIN LATERAL ( SELECT string_to_array(l.appointment_address,',') AS VALUE ) address_array
ON TRUE JOIN tmp_uk_counties AS counties ON LOWER (counties.county) = TRIM (LOWER (address_array.value[array_length(address_array.value, 1)]));

-- delete processed data from last source table
DELETE
FROM tmp_stage_2 USING tmp_stage_3
WHERE tmp_stage_3.id = tmp_stage_2.id;

INSERT INTO tmp_stage_3 (id,appointment_address,address_part_length,postcode,postcode_prefix,county,country)
SELECT id,
	   appointment_address,
	   address_part_length,
	   postcode,
	   postcode_prefix,
	   counties.county,
	   COALESCE(l.country,counties.country)
FROM tmp_stage_2 l
		 LEFT JOIN LATERAL ( SELECT string_to_array(l.appointment_address,',') AS VALUE ) address_array
ON TRUE JOIN tmp_uk_counties AS counties ON LOWER (counties.county) = TRIM (LOWER (address_array.value[array_length(address_array.value, 1)-1]));

DELETE
FROM tmp_stage_2 USING tmp_stage_3
WHERE tmp_stage_3.id = tmp_stage_2.id;

INSERT INTO tmp_stage_3 (id,appointment_address,address_part_length,postcode,postcode_prefix,county,country)
SELECT id,
	   appointment_address,
	   address_part_length,
	   postcode,
	   postcode_prefix,
	   counties.county,
	   COALESCE(l.country,counties.country)
FROM tmp_stage_2 l
		 LEFT JOIN LATERAL ( SELECT string_to_array(l.appointment_address,',') AS VALUE ) address_array
ON TRUE JOIN tmp_uk_counties AS counties ON LOWER (counties.county) = TRIM (LOWER (address_array.value[array_length(address_array.value, 1)-2]));

DELETE
FROM tmp_stage_2 USING tmp_stage_3
WHERE tmp_stage_2.id = tmp_stage_3.id;

INSERT INTO tmp_stage_3 (id,appointment_address,address_part_length,postcode,postcode_prefix,county,country)
SELECT id,
	   appointment_address,
	   address_part_length,
	   postcode,
	   postcode_prefix,
	   counties.county,
	   COALESCE(l.country,counties.country)
FROM tmp_stage_2 l
		 LEFT JOIN LATERAL ( SELECT string_to_array(l.appointment_address,',') AS VALUE ) address_array
ON TRUE JOIN tmp_uk_counties AS counties ON LOWER (counties.county) = TRIM (LOWER (address_array.value[array_length(address_array.value, 1)-3]));

DELETE
FROM tmp_stage_2 USING tmp_stage_3
WHERE tmp_stage_2.id = tmp_stage_3.id;

-- adds un processed data
INSERT INTO tmp_stage_3 (id,appointment_address,address_part_length,postcode,postcode_prefix,county,country)
SELECT id, appointment_address, address_part_length, postcode, postcode_prefix, NULL, country
FROM tmp_stage_2 l;

DELETE
FROM tmp_stage_2 USING tmp_stage_3
WHERE tmp_stage_2.id = tmp_stage_3.id;

-- A table of Main cities and towns with county, country and pre fix

CREATE
TEMP TABLE tmp_uk_urban_places (
  urban_name TEXT NOT NULL,
  place_type TEXT NOT NULL,  -- 'City' or 'Town'
  country TEXT NOT NULL,     -- ENGLAND, SCOTLAND, WALES, NORTHEN_IRELAND
  county TEXT NOT NULL,       -- Ceremonial / administrative county
  urban_postcode_prefix text NULL
);

INSERT INTO tmp_uk_urban_places (urban_name,place_type,country,county,urban_postcode_prefix)
VALUES
-- Cities (alphabetical)
('Bath','City','ENGLAND','Somerset','BA'),
('Belfast','City','NORTHEN_IRELAND','County Antrim','BT'),
('Birmingham','City','ENGLAND','West Midlands','B'),
('Bradford','City','ENGLAND','West Yorkshire','BD'),
('Brighton and Hove','City','ENGLAND','East Sussex','BN'),
('Bristol','City','ENGLAND','Bristol','BS'),
('Cambridge','City','ENGLAND','Cambridgeshire','CB'),
('Canterbury','City','ENGLAND','Kent','CT'),
('Cardiff','City','WALES','Cardiff','CF'),
('Carlisle','City','ENGLAND','Cumbria','CA'),
('Chelmsford','City','ENGLAND','Essex','CM'),
('Chester','City','ENGLAND','Cheshire','CH'),
('Chichester','City','ENGLAND','West Sussex','PO'),
('Coventry','City','ENGLAND','West Midlands','CV'),
('Derby','City','ENGLAND','Derbyshire','DE'),
('Derry','City','NORTHEN_IRELAND','County Londonderry','BT'),
('Durham','City','ENGLAND','County Durham','DH'),
('Ely','City','ENGLAND','Cambridgeshire','CB'),
('Edinburgh','City','SCOTLAND','City of Edinburgh','EH'),
('Exeter','City','ENGLAND','Devon','EX'),
('Glasgow','City','SCOTLAND','Glasgow City','G'),
('Gloucester','City','ENGLAND','Gloucestershire','GL'),
('Hull','City','ENGLAND','East Riding of Yorkshire','HU'),
('Inverness','City','SCOTLAND','Highland','IV'),
('Kingston upon Hull','City','ENGLAND','East Riding of Yorkshire','HU'),
('Lancaster','City','ENGLAND','Lancashire','LA'),
('Leeds','City','ENGLAND','West Yorkshire','LS'),
('Leicester','City','ENGLAND','Leicestershire','LE'),
('Lichfield','City','ENGLAND','Staffordshire','WS'),
('Lincoln','City','ENGLAND','Lincolnshire','LN'),
('Lisburn','City','NORTHEN_IRELAND','County Antrim','BT'),
('Liverpool','City','ENGLAND','Merseyside','L'),
('London','City','ENGLAND','Greater London',NULL), -- Multiple prefixes
('Manchester','City','ENGLAND','Greater Manchester','M'),
('Newcastle upon Tyne','City','ENGLAND','Tyne and Wear','NE'),
('Newport','City','WALES','Newport','NP'),
('Norwich','City','ENGLAND','Norfolk','NR'),
('Nottingham','City','ENGLAND','Nottinghamshire','NG'),
('Oxford','City','ENGLAND','Oxfordshire','OX'),
('Peterborough','City','ENGLAND','Cambridgeshire','PE'),
('Perth','City','SCOTLAND','Perth and Kinross','PH'),
('Plymouth','City','ENGLAND','Devon','PL'),
('Portsmouth','City','ENGLAND','Hampshire','PO'),
('Preston','City','ENGLAND','Lancashire','PR'),
('Ripon','City','ENGLAND','North Yorkshire','HG'),
('Salford','City','ENGLAND','Greater Manchester','M'),
('Salisbury','City','ENGLAND','Wiltshire','SP'),
('Sheffield','City','ENGLAND','South Yorkshire','S'),
('Southampton','City','ENGLAND','Hampshire','SO'),
('St Albans','City','ENGLAND','Hertfordshire','AL'),
('St Andrews','City','SCOTLAND','Fife','KY'),
('St Davids','City','WALES','Pembrokeshire','SA'),
('Stirling','City','SCOTLAND','Stirling','FK'),
('Swansea','City','WALES','Swansea','SA'),
('Truro','City','ENGLAND','Cornwall','TR'),
('Wakefield','City','ENGLAND','West Yorkshire','WF'),
('Wells','City','ENGLAND','Somerset','BA'),
('Winchester','City','ENGLAND','Hampshire','SO'),
('Wolverhampton','City','ENGLAND','West Midlands','WV'),
('Worcester','City','ENGLAND','Worcestershire','WR'),
('York','City','ENGLAND','North Yorkshire','YO'),

-- Towns (alphabetical)
('Abingdon on Thames','Town','ENGLAND','Oxfordshire','OX'),
('Aberdare','Town','WALES','Rhondda Cynon Taf','CF'),
('Accrington','Town','ENGLAND','Lancashire','BB'),
('Alexandria','Town','SCOTLAND','West Dunbartonshire','G'),
('Alfreton','Town','ENGLAND','Derbyshire','DE'),
('Antrim','Town','NORTHEN_IRELAND','County Antrim','BT'),
('Andover','Town','ENGLAND','Hampshire','SP'),
('Armagh','City','NORTHEN_IRELAND','County Armagh','BT'),
('Ashford','Town','ENGLAND','Kent','TN'),
('Aylesbury','Town','ENGLAND','Buckinghamshire','HP'),
('Banbury','Town','ENGLAND','Oxfordshire','OX'),
('Bangor','City','WALES','Gwynedd','LL'),
-- ('Bangor', 'Town', 'NORTHEN_IRELAND', 'County Down', 'BT'),
('Barnsley','Town','ENGLAND','South Yorkshire','S'),
('Barnstaple','Town','ENGLAND','Devon','EX'),
('Barrow in Furness','Town','ENGLAND','Cumbria','LA'),
('Barrhead','Town','SCOTLAND','East Renfrewshire','G'),
('Barry','Town','WALES','Vale of Glamorgan','CF'),
('Basildon','Town','ENGLAND','Essex','SS'),
('Basingstoke','Town','ENGLAND','Hampshire','RG'),
('Bathgate','Town','SCOTLAND','West Lothian','EH'),
('Bellshill','Town','SCOTLAND','North Lanarkshire','ML'),
('Bingley','Town','ENGLAND','West Yorkshire','BD'),
('Birkenhead','Town','ENGLAND','Merseyside','CH'),
('Blackburn','Town','ENGLAND','Lancashire','BB'),
('Blackpool','Town','ENGLAND','Lancashire','FY'),
('Blyth','Town','ENGLAND','Northumberland','NE'),
('Bognor Regis','Town','ENGLAND','West Sussex','PO'),
('Boston','Town','ENGLAND','Lincolnshire','PE'),
('Bournemouth','Town','ENGLAND','Dorset','BH'),
('Brighton','Town','ENGLAND','East Sussex','BN'),
('Broxburn','Town','SCOTLAND','West Lothian','EH'),
('Bromsgrove','Town','ENGLAND','Worcestershire','B'),
('Burnley','Town','ENGLAND','Lancashire','BB'),
('Burton upon Trent','Town','ENGLAND','Staffordshire','DE'),
('Bury','Town','ENGLAND','Greater Manchester','BL'),
('Bolton','Town','ENGLAND','Greater Manchester','BL'),
('Caerphilly','Town','WALES','Caerphilly','CF'),
('Calne','Town','ENGLAND','Wiltshire','SN'),
('Camborne','Town','ENGLAND','Cornwall','TR'),
('Cannock','Town','ENGLAND','Staffordshire','WS'),
('Carrickfergus','Town','NORTHEN_IRELAND','County Antrim','BT'),
('Castleford','Town','ENGLAND','West Yorkshire','WF'),
('Chesterfield','Town','ENGLAND','Derbyshire','S'),
('Chippenham','Town','ENGLAND','Wiltshire','SN'),
('Clacton on Sea','Town','ENGLAND','Essex','CO'),
('Coatbridge','Town','SCOTLAND','North Lanarkshire','ML'),
('Colchester','Town','ENGLAND','Essex','CO'),
('Coleraine','Town','NORTHEN_IRELAND','County Londonderry','BT'),
('Consett','Town','ENGLAND','County Durham','DH'),
('Corby','Town','ENGLAND','Northamptonshire','NN'),
('Crawley','Town','ENGLAND','West Sussex','RH'),
('Crewe','Town','ENGLAND','Cheshire','CW'),
('Croydon','Town','ENGLAND','Greater London','CR'),
('Cwmbran','Town','WALES','Torfaen','NP'),
('Darlington','Town','ENGLAND','County Durham','DL'),
('Dewsbury','Town','ENGLAND','West Yorkshire','WF'),
('Doncaster','Town','ENGLAND','South Yorkshire','DN'),
('Downpatrick','City','NORTHEN_IRELAND','County Down','BT'),
('Dover','Town','ENGLAND','Kent','CT'),
('Dumbarton','Town','SCOTLAND','West Dunbartonshire','G'),
('Dumfries','Town','SCOTLAND','Dumfries and Galloway','DG'),
('Eastbourne','Town','ENGLAND','East Sussex','BN'),
('Elgin','Town','SCOTLAND','Moray','IV'),
('Ellesmere Port','Town','ENGLAND','Cheshire','CH'),
('Exmouth','Town','ENGLAND','Devon','EX'),
('Farnborough','Town','ENGLAND','Hampshire','GU'),
('Felixstowe','Town','ENGLAND','Suffolk','IP'),
('Guildford','Town','ENGLAND','Surrey','GU'),
('Gateshead','Town','ENGLAND','Tyne and Wear','NE'),
('Halifax','Town','ENGLAND','West Yorkshire','HX'),
('Harlow','Town','ENGLAND','Essex','CM'),
('Hartlepool','Town','ENGLAND','County Durham','TS'),
('Hereford','City','ENGLAND','Herefordshire','HR'),
('High Wycombe','Town','ENGLAND','Buckinghamshire','HP'),
('Huddersfield','Town','ENGLAND','West Yorkshire','HD'),
('Ipswich','Town','ENGLAND','Suffolk','IP'),
('Kingston upon Thames','Town','ENGLAND','Greater London','KT'),
('Kirkintilloch','Town','SCOTLAND','East Dunbartonshire','G'),
('Larne','Town','NORTHEN_IRELAND','County Antrim','BT'),
('Llanelli','Town','WALES','Carmarthenshire','SA'),
('Londonderry','Town','NORTHEN_IRELAND','County Londonderry','BT'),
('Maidstone','Town','ENGLAND','Kent','ME'),
('Mansfield','Town','ENGLAND','Nottinghamshire','NG'),
('Medway Towns','Town','ENGLAND','Kent','ME'),
('Merthyr Tydfil','Town','WALES','Merthyr Tydfil','CF'),
('Middlesbrough','Town','ENGLAND','North Yorkshire','TS'),
('Milton Keynes','Town','ENGLAND','Buckinghamshire','MK'),
('Morecambe','Town','ENGLAND','Lancashire','LA'),
('Musselburgh','Town','SCOTLAND','East Lothian','EH'),
('Neath','Town','WALES','Neath Port Talbot','SA'),
('Newry','City','NORTHEN_IRELAND','County Down','BT'),
('Newtownabbey','Town','NORTHEN_IRELAND','County Antrim','BT'),
('Oldham','Town','ENGLAND','Greater Manchester','OL'),
('Penicuik','Town','SCOTLAND','Midlothian','EH'),
('Peterhead','Town','SCOTLAND','Aberdeenshire','AB'),
('Poole','Town','ENGLAND','Dorset','BH'),
('Pontypridd','Town','WALES','Rhondda Cynon Taf','CF'),
('Reading','Town','ENGLAND','Berkshire','RG'),
('Renfrew','Town','SCOTLAND','Renfrewshire','PA'),
('Rochdale','Town','ENGLAND','Greater Manchester','OL'),
('Rotherham','Town','ENGLAND','South Yorkshire','S'),
('Saint Helens','Town','ENGLAND','Merseyside','WA'),
('Sale','Town','ENGLAND','Greater Manchester','M'),
('Scarborough','Town','ENGLAND','North Yorkshire','YO'),
('Slough','Town','ENGLAND','Berkshire','SL'),
('Solihull','Town','ENGLAND','West Midlands','B'),
('Southend on Sea','Town','ENGLAND','Essex','SS'),
('St Asaph','City','WALES','Denbighshire','LL'),
('Stoke on Trent','Town','ENGLAND','Staffordshire','ST'),
('Stockport','Town','ENGLAND','Greater Manchester','SK'),
('Sunderland','Town','ENGLAND','Tyne and Wear','SR'),
('Swindon','Town','ENGLAND','Wiltshire','SN'),
('Telford','Town','ENGLAND','Shropshire','TF'),
('Uddingston','Town','SCOTLAND','South Lanarkshire','G'),
('Warrington','Town','ENGLAND','Cheshire','WA'),
('Wigan','Town','ENGLAND','Greater Manchester','WN'),
('Wishaw','Town','SCOTLAND','North Lanarkshire','ML'),
('Wrexham','Town','WALES','Wrexham','LL'),
('Richmond','Town','ENGLAND','Greater London','TW'),
('Hornchurch','Town','ENGLAND','Greater London','RM'),
('Northampton','Town','ENGLAND','Northamptonshire','NN'),
('West Bromwich','Town','ENGLAND','West Midland','B'),
('Walsall','Town','ENGLAND','West Midland','WS'),
('Luton','Town','ENGLAND','Bedfordshire','LU'),
('Atherton','Town','ENGLAND','Greater Manchester','M'),
('Scunthorpe','Town','ENGLAND','North Lincolnshire','DN'),
('Nuneaton','Town','ENGLAND','Warwickshire','CV'),
('Lewisham','Town','ENGLAND','Greater London','SE'),
('Torquay','Town','ENGLAND','Devon','TQ'),
('Prescot','Town','ENGLAND','Merseyside','L'),
('Aldershot','Town','ENGLAND','Hampshire','GU'),
('Aberdeen','City','SCOTLAND','Aberdeen City','AB'),
('Dundee','City','SCOTLAND','Dundee City','DD'),
('Watford','Town','ENGLAND','Hertfordshire','WD'),
('Stafford','Town','ENGLAND','Staffordshire','ST'),
('Conwy','Town','WALES','Conwy','LL'),
('Chatham','Town','ENGLAND','Kent','ME'),
('Stockton','Town','ENGLAND','County Durham','TS'),
('Laindon','Town','ENGLAND','Essex','SS'),
('Folkestone','Town','ENGLAND','Kent','CT'),
('Shrewsbury','Town','ENGLAND','Shropshire','SY'),
('Haverfordwest','Town','WALES','Pembrokeshire','SA'),
('Bridgend','Town','WALES','Bridgend','CF'),
('Newport (Isle of Wight)','Town','ENGLAND','Isle of Wight','PO'),
('Royal Leamington Spa','Town','ENGLAND','Warwickshire','CV'),
('Redditch','Town','ENGLAND','Worcestershire','B'),
('Kettering','Town','ENGLAND','Northamptonshire','NN'),
('Grimsby','Town','ENGLAND','Lincolnshire','DN'),
('Harrogate','Town','ENGLAND','North Yorkshire','HG'),
('Skegness','Town','ENGLAND','Lincolnshire','PE'),
('Rugby','Town','ENGLAND','Warwickshire','CV'),
('King’s Lynn','Town','ENGLAND','Norfolk','PE'),
('Worksop','Town','ENGLAND','Nottinghamshire','S'),
('Bishop Auckland','Town','ENGLAND','County Durham','DL'),
('Hastings','Town','ENGLAND','East Sussex','TN'),
('Morpeth','Town','ENGLAND','Northumberland','NE'),
('Bracknell','Town','ENGLAND','Berkshire','RG'),
('Banbridge','Town','NORTHEN_IRELAND','County Down','BT'),
('Motherwell','Town','SCOTLAND','North Lanarkshire','ML'),
('Ayr','Town','SCOTLAND','South Ayrshire','KA'),
('Loughborough','Town','ENGLAND','Leicestershire','LE'),
('Greenock','Town','SCOTLAND','Inverclyde','PA'),
('Alnwick','Town','ENGLAND','Northumberland','NE'),
('Arbroath','Town','SCOTLAND','Angus','DD'),
('Ashington','Town','ENGLAND','Northumberland','NE'),
('Beccles','Town','ENGLAND','Suffolk','NR'),
('Beverley','Town','ENGLAND','East Riding of Yorkshire','HU'),
('Bideford','Town','ENGLAND','Devon','EX'),
('Biggleswade','Town','ENGLAND','Bedfordshire','SG'),
('Bishop’s Stortford','Town','ENGLAND','Hertfordshire','CM'),
('Blaydon','Town','ENGLAND','Tyne and Wear','NE'),
('Bridport','Town','ENGLAND','Dorset','DT'),
('Buckfastleigh','Town','ENGLAND','Devon','TQ'),
('Burntisland','Town','SCOTLAND','Fife','KY'),
('Caithness','Town','SCOTLAND','Highland','KW'),
('Carnoustie','Town','SCOTLAND','Angus','DD'),
('Castlereagh','Town','NORTHEN_IRELAND','County Down','BT'),
('Chipping Norton','Town','ENGLAND','Oxfordshire','OX'),
('Coalville','Town','ENGLAND','Leicestershire','LE'),
('Cowbridge','Town','WALES','Vale of Glamorgan','CF'),
('Cromer','Town','ENGLAND','Norfolk','NR'),
('Cupar','Town','SCOTLAND','Fife','KY'),
('Dalkeith','Town','SCOTLAND','Midlothian','EH'),
('Dunbar','Town','SCOTLAND','East Lothian','EH'),
('Dunfermline','Town','SCOTLAND','Fife','KY'),
('Dunkeld','Town','SCOTLAND','Perth and Kinross','PH'),
('Easingwold','Town','ENGLAND','North Yorkshire','YO'),
('Epsom','Town','ENGLAND','Surrey','KT'),
('Fakenham','Town','ENGLAND','Norfolk','NR'),
('Filey','Town','ENGLAND','North Yorkshire','YO'),
('Forfar','Town','SCOTLAND','Angus','DD'),
('Fort William','Town','SCOTLAND','Highland','PH'),
('Galashiels','Town','SCOTLAND','Scottish Borders','TD'),
('Gosport','Town','ENGLAND','Hampshire','PO'),
('Grangemouth','Town','SCOTLAND','Falkirk','FK'),
('Havant','Town','ENGLAND','Hampshire','PO'),
('Helensburgh','Town','SCOTLAND','Argyll and Bute','G'),
('Holyhead','Town','WALES','Anglesey','LL'),
('Huntingdon','Town','ENGLAND','Cambridgeshire','PE'),
('Inverurie','Town','SCOTLAND','Aberdeenshire','AB'),
('Jedburgh','Town','SCOTLAND','Scottish Borders','TD'),
('Keighley','Town','ENGLAND','West Yorkshire','BD'),
('Kendal','Town','ENGLAND','Cumbria','LA'),
('Kelso','Town','SCOTLAND','Scottish Borders','TD'),
('Kilwinning','Town','SCOTLAND','North Ayrshire','KA'),
('Knaresborough','Town','ENGLAND','North Yorkshire','HG'),
('Lanark','Town','SCOTLAND','South Lanarkshire','ML'),
('Liskeard','Town','ENGLAND','Cornwall','PL'),
('Lossiemouth','Town','SCOTLAND','Moray','IV'),
('Louth','Town','ENGLAND','Lincolnshire','LN'),
('Maldon','Town','ENGLAND','Essex','CM'),
('Melrose','Town','SCOTLAND','Scottish Borders','TD'),
('Moffat','Town','SCOTLAND','Dumfries and Galloway','DG'),
('Montrose','Town','SCOTLAND','Angus','DD'),
('Moreton-in-Marsh','Town','ENGLAND','Gloucestershire','GL'),
('Nairn','Town','SCOTLAND','Highland','IV'),
('Nantwich','Town','ENGLAND','Cheshire','CW'),
('Newark-on-Trent','Town','ENGLAND','Nottinghamshire','NG'),
('Newburgh','Town','SCOTLAND','Fife','KY'),
('Newcastle Emlyn','Town','WALES','Ceredigion','SA'),
('Northallerton','Town','ENGLAND','North Yorkshire','DL'),
('Northwich','Town','ENGLAND','Cheshire','CW'),
('Oban','Town','SCOTLAND','Argyll and Bute','PA'),
('Okehampton','Town','ENGLAND','Devon','EX'),
('Old Kilpatrick','Town','SCOTLAND','West Dunbartonshire','G'),
('Ormskirk','Town','ENGLAND','Merseyside','L'),
('Peebles','Town','SCOTLAND','Scottish Borders','EH'),
('Penrith','Town','ENGLAND','Cumbria','CA'),
('Penzance','Town','ENGLAND','Cornwall','TR'),
('Pitlochry','Town','SCOTLAND','Perth and Kinross','PH'),
('Prestwick','Town','SCOTLAND','South Ayrshire','KA'),
('Redruth','Town','ENGLAND','Cornwall','TR'),
('Rothbury','Town','ENGLAND','Northumberland','NE'),
('Saltburn-by-the-Sea','Town','ENGLAND','North Yorkshire','TS'),
('Sittingbourne','Town','ENGLAND','Kent','ME'),
('Skelmersdale','Town','ENGLAND','Lancashire','WN'),
('South Molton','Town','ENGLAND','Devon','EX'),
('South Shields','Town','ENGLAND','Tyne and Wear','NE'),
('Stonehaven','Town','SCOTLAND','Aberdeenshire','AB'),
('Stornoway','Town','SCOTLAND','Western Isles','HS'),
('Stowmarket','Town','ENGLAND','Suffolk','IP'),
('Stranraer','Town','SCOTLAND','Dumfries and Galloway','DG'),
('Sutton Coldfield','Town','ENGLAND','West Midlands','B'),
('Tavistock','Town','ENGLAND','Devon','PL'),
('Tenterden','Town','ENGLAND','Kent','TN'),
('Thurso','Town','SCOTLAND','Highland','KW'),
('Tonbridge','Town','ENGLAND','Kent','TN'),
('Troon','Town','SCOTLAND','South Ayrshire','KA'),
('Urmston','Town','ENGLAND','Greater Manchester','M'),
('Uttoxeter','Town','ENGLAND','Staffordshire','ST'),
('West Kirby','Town','ENGLAND','Merseyside','CH'),
('Wetherby','Town','ENGLAND','West Yorkshire','LS'),
('Whitby','Town','ENGLAND','North Yorkshire','YO'),
('Wick','Town','SCOTLAND','Highland','KW'),
('Wigtown','Town','SCOTLAND','Dumfries and Galloway','DG'),
('Witney','Town','ENGLAND','Oxfordshire','OX'),
('Worsborough','Town','ENGLAND','South Yorkshire','S'),
('Acton','Town','ENGLAND','Greater London','W'),
('Ashton-under-Lyne','Town','ENGLAND','Greater Manchester','OL'),
('Bexleyheath','Town','ENGLAND','Greater London','DA'),
('Bootle','Town','ENGLAND','Merseyside','L'),
('Buxton','Town','ENGLAND','Derbyshire','SK'),
('Caernarfon','Town','WALES','Gwynedd','LL'),
('Dudley','Town','ENGLAND','West Midlands','DY'),
('Eccles','Town','ENGLAND','Greater Manchester','M'),
('Huyton','Town','ENGLAND','Merseyside','L'),
('Ilford','Town','ENGLAND','Greater London','IG'),
('Kidderminster','Town','ENGLAND','Worcestershire','DY'),
('Leytonstone','Town','ENGLAND','Greater London','E'),
('Lowestoft','Town','ENGLAND','Suffolk','NR'),
('Newcastle','City','ENGLAND','Tyne and Wear','NE'),
('Oldbury','Town','ENGLAND','West Midlands','B'),
('Orpington','Town','ENGLAND','Kent','BR'),
('Peterlee','Town','ENGLAND','County Durham','SR'),
('Purbrook','Town','ENGLAND','Hampshire','PO'),
('Ramsgate','Town','ENGLAND','Kent','CT'),
('St Leonards on Sea','Town','ENGLAND','East Sussex','TN'),
('Staines','Town','ENGLAND','Surrey','TW'),
('Tamworth','Town','ENGLAND','Staffordshire','B'),
('Taunton','Town','ENGLAND','Somerset','TA'),
('Tunbridge Wells','Town','ENGLAND','Kent','TN'),
('Uxbridge','Town','ENGLAND','Greater London','UB'),
('Waterlooville','Town','ENGLAND','Hampshire','PO'),
('Weymouth','Town','ENGLAND','Dorset','DT'),
('Workington','Town','ENGLAND','Cumbria','CA'),
('Yeovil','Town','ENGLAND','Somerset','BA'),
('Leamington Spa','Town','ENGLAND','Warwickshire','CV'),
('Stevenage','Town','ENGLAND','Hertfordshire','SG'),
('Wallsend','Town','ENGLAND','Tyne and Wear','NE'),
('Aberystwyth', 'Town', 'Wales', 'Ceredigion', 'SY'),
('Bedford', 'Town', 'England', 'Bedfordshire', 'MK'),
('Chorley', 'Town', 'England', 'Lancashire', 'PR'),
('Dorchester', 'Town', 'England', 'Dorset', 'DT'),
('Gainsborough', 'Town', 'England', 'Lincolnshire', 'DN'),
('Hexham', 'Town', 'England', 'Northumberland', 'NE'),
('Redcar', 'Town', 'England', 'North Yorkshire', 'TS'),
('Rhyl', 'Town', 'Wales', 'Denbighshire', 'LL'),
('Skipton', 'Town', 'England', 'North Yorkshire', 'BD'),
('Worthing', 'Town', 'England', 'West Sussex', 'BN');


-- Stage 4 gets urban.urban_postcode_prefix not already aquired and country, county, urban_name

CREATE
TEMP TABLE tmp_stage_4 AS
SELECT id,
	   appointment_address,
	   address_part_length,
	   postcode,
	   COALESCE(postcode_prefix,urban.urban_postcode_prefix) AS postcode_prefix,
	   COALESCE(l.country,urban.country) AS country,
	   COALESCE(l.county,urban.county) AS county,
	   urban.urban_name
FROM tmp_stage_3 l
		 LEFT JOIN LATERAL ( SELECT string_to_array(l.appointment_address,',') AS VALUE ) address_array
ON TRUE LEFT JOIN LATERAL ( SELECT LOWER (regexp_replace(address_array.value[array_length(address_array.value, 1)], '[^A-Za-z]', '', 'g')) AS VALUE ) address_element_cleaned ON TRUE JOIN tmp_uk_urban_places AS urban ON LOWER (regexp_replace(urban.urban_name, '[^A-Za-z]', '', 'g')) = address_element_cleaned.value;

-- delete processed data from last source table
DELETE
FROM tmp_stage_3 USING tmp_stage_4
WHERE tmp_stage_3.id = tmp_stage_4.id;

INSERT INTO tmp_stage_4 (id,appointment_address,address_part_length,postcode,postcode_prefix,country,county,
						 urban_name)
SELECT id,
	   appointment_address,
	   address_part_length,
	   postcode,
	   COALESCE(postcode_prefix,urban.urban_postcode_prefix) AS postcode_prefix,
	   COALESCE(l.country,urban.country) AS country,
	   COALESCE(l.county,urban.county) AS county,
	   urban.urban_name
FROM tmp_stage_3 l
		 LEFT JOIN LATERAL ( SELECT string_to_array(l.appointment_address,',') AS VALUE ) address_array
ON TRUE LEFT JOIN LATERAL ( SELECT LOWER (regexp_replace(address_array.value[array_length(address_array.value, 1)-1], '[^A-Za-z]', '', 'g')) AS VALUE ) address_element_cleaned ON TRUE JOIN tmp_uk_urban_places AS urban ON LOWER (regexp_replace(urban.urban_name, '[^A-Za-z]', '', 'g')) = address_element_cleaned.value;

DELETE
FROM tmp_stage_3 USING tmp_stage_4
WHERE tmp_stage_3.id = tmp_stage_4.id;

INSERT INTO tmp_stage_4 (id,appointment_address,address_part_length,postcode,postcode_prefix,country,county,
						 urban_name)
SELECT id,
	   appointment_address,
	   address_part_length,
	   postcode,
	   COALESCE(postcode_prefix,urban.urban_postcode_prefix) AS postcode_prefix,
	   COALESCE(l.country,urban.country) AS country,
	   COALESCE(l.county,urban.county) AS county,
	   urban.urban_name
FROM tmp_stage_3 l
		 LEFT JOIN LATERAL ( SELECT string_to_array(l.appointment_address,',') AS VALUE ) address_array
ON TRUE LEFT JOIN LATERAL ( SELECT LOWER (regexp_replace(address_array.value[array_length(address_array.value, 1)-2], '[^A-Za-z]', '', 'g')) AS VALUE ) address_element_cleaned ON TRUE JOIN tmp_uk_urban_places AS urban ON LOWER (regexp_replace(urban.urban_name, '[^A-Za-z]', '', 'g')) = address_element_cleaned.value;

DELETE
FROM tmp_stage_3 USING tmp_stage_4
WHERE tmp_stage_3.id = tmp_stage_4.id;

INSERT INTO tmp_stage_4 (id,appointment_address,address_part_length,postcode,postcode_prefix,country,county,
						 urban_name)
SELECT id,
	   appointment_address,
	   address_part_length,
	   postcode,
	   COALESCE(postcode_prefix,urban.urban_postcode_prefix) AS postcode_prefix,
	   COALESCE(l.country,urban.country) AS country,
	   COALESCE(l.county,urban.county) AS county,
	   urban.urban_name
FROM tmp_stage_3 l
		 LEFT JOIN LATERAL ( SELECT string_to_array(l.appointment_address,',') AS VALUE ) address_array
ON TRUE LEFT JOIN LATERAL ( SELECT LOWER (regexp_replace(address_array.value[array_length(address_array.value, 1)-3], '[^A-Za-z]', '', 'g')) AS VALUE ) address_element_cleaned ON TRUE JOIN tmp_uk_urban_places AS urban ON LOWER (regexp_replace(urban.urban_name, '[^A-Za-z]', '', 'g')) = address_element_cleaned.value;


DELETE
FROM tmp_stage_3 USING tmp_stage_4
WHERE tmp_stage_3.id = tmp_stage_4.id;


INSERT INTO tmp_stage_4 (id,appointment_address,address_part_length,postcode,postcode_prefix,country,county,
						 urban_name)
SELECT id,
	   appointment_address,
	   address_part_length,
	   postcode,
	   COALESCE(postcode_prefix,urban.urban_postcode_prefix) AS postcode_prefix,
	   COALESCE(l.country,urban.country) AS country,
	   COALESCE(l.county,urban.county) AS county,
	   urban.urban_name
FROM tmp_stage_3 l
		 LEFT JOIN LATERAL ( SELECT string_to_array(l.appointment_address,',') AS VALUE ) address_array
ON TRUE LEFT JOIN LATERAL ( SELECT LOWER (regexp_replace(address_array.value[array_length(address_array.value, 1)-4], '[^A-Za-z]', '', 'g')) AS VALUE ) address_element_cleaned ON TRUE JOIN tmp_uk_urban_places AS urban ON LOWER (regexp_replace(urban.urban_name, '[^A-Za-z]', '', 'g')) = address_element_cleaned.value;


DELETE
FROM tmp_stage_3 USING tmp_stage_4
WHERE tmp_stage_3.id = tmp_stage_4.id;

-- adds un processed data
INSERT INTO tmp_stage_4 (id,appointment_address,address_part_length,postcode,postcode_prefix,county,country,
						 urban_name)
SELECT id,
	   appointment_address,
	   address_part_length,
	   postcode,
	   postcode_prefix,
	   county,
	   country,
	   NULL
FROM tmp_stage_3 l;

DELETE
FROM tmp_stage_3 USING tmp_stage_4
WHERE tmp_stage_3.id = tmp_stage_4.id;

-- Common addresses to match with

CREATE
TEMPORARY TABLE tmp_common_addresses (
    appointment_address TEXT,
    address_line_1 TEXT,
    address_line_2 TEXT,
    urban_name TEXT,
    county TEXT,
    postcode TEXT,
    postcode_prefix TEXT,
    country TEXT
);


INSERT INTO tmp_common_addresses (appointment_address,address_line_1,address_line_2,urban_name,county,postcode,
								  postcode_prefix,country)
VALUES (<TO_BE_FOUND_ON_TICKET>);
-- https://dsdmoj.atlassian.net/browse/CVSL-2964 in comment section

-- Stage 5 finds regular address and adds urban_name postcode postcode_prefix county country if present

CREATE
TEMP TABLE tmp_stage_5 AS
SELECT id,
	   l.appointment_address,
	   l.address_part_length,
	   common.address_line_1,
	   common.address_line_2,
	   COALESCE(common.urban_name,l.urban_name) AS urban_name,
	   COALESCE(common.postcode,l.postcode) AS postcode,
	   COALESCE(common.postcode_prefix,l.postcode_prefix) AS postcode_prefix,
	   COALESCE(common.county,l.county) AS county,
	   COALESCE(common.country,l.country) AS country,
	   TRUE AS complete
FROM tmp_stage_4 l
		 LEFT JOIN LATERAL ( SELECT regexp_replace(TRIM(LOWER(l.appointment_address)),'[^A-Za-z0-9 ,]','',
												   'g') AS VALUE ) cleaned
ON TRUE JOIN tmp_common_addresses AS common ON regexp_replace(TRIM (LOWER (common.appointment_address)), '[^A-Za-z0-9 ,]', '', 'g') = cleaned.value;

-- delete processed data from last source table
DELETE
FROM tmp_stage_4 USING tmp_stage_5
WHERE tmp_stage_4.id = tmp_stage_5.id;

-- adds un processed data
INSERT INTO tmp_stage_5 (id,appointment_address,address_part_length,address_line_1,address_line_2,postcode,
						 postcode_prefix,county,country,urban_name,complete)
SELECT id,
	   appointment_address,
	   address_part_length,
	   NULL,
	   NULL,
	   postcode,
	   postcode_prefix,
	   county,
	   country,
	   urban_name,
	   FALSE
FROM tmp_stage_4 l;

DELETE
FROM tmp_stage_4 USING tmp_stage_5
WHERE tmp_stage_4.id = tmp_stage_5.id;

-- Stage 6 address_line_1 and address_line_2, the first line as only one element the second has all the remaining
-- This takes a while 10 seconds +
CREATE TEMP TABLE tmp_stage_6 AS
WITH cleaned AS (
  SELECT
    t.id,
    t.county,
    t.urban_name,
    t.country,
    t.postcode,
    coalesce(STRING_AGG(TRIM(e.value), ', '),'') AS cleaned_appointment_address,
    t.appointment_address
  FROM tmp_stage_5 t,
  LATERAL unnest(string_to_array(t.appointment_address, ',')) AS e(value)
  WHERE
    REGEXP_REPLACE(LOWER(TRIM(e.value)), '[^a-z0-9]', '', 'g') IS DISTINCT FROM REGEXP_REPLACE(LOWER(TRIM(t.county)), '[^a-z0-9]', '', 'g') AND
    REGEXP_REPLACE(LOWER(TRIM(e.value)), '[^a-z0-9]', '', 'g') IS DISTINCT FROM REGEXP_REPLACE(LOWER(TRIM(t.urban_name)), '[^a-z0-9]', '', 'g') AND
    REGEXP_REPLACE(LOWER(TRIM(e.value)), '[^a-z0-9]', '', 'g') IS DISTINCT FROM REGEXP_REPLACE(LOWER(TRIM(t.country)), '[^a-z0-9]', '', 'g') AND
    REGEXP_REPLACE(LOWER(TRIM(e.value)), '[^a-z0-9]', '', 'g') IS DISTINCT FROM REGEXP_REPLACE(LOWER(TRIM(t.postcode)), '[^a-z0-9]', '', 'g')
  GROUP BY t.id, t.appointment_address, t.county, t.urban_name, t.country, t.postcode
)
SELECT
	id,
	appointment_address,
	cleaned_appointment_address,
	cardinality(string_to_array(cleaned_appointment_address, ',')) AS address_part_length,
	-- First part of the address
	TRIM(SPLIT_PART(cleaned_appointment_address, ',', 1)) AS address_line_1,
	-- Everything after the first comma, or NULL if none
	CASE
		WHEN cleaned_appointment_address IS NULL THEN NULL
		WHEN POSITION(',' IN cleaned_appointment_address) = 0 THEN NULL
		ELSE LTRIM(SUBSTRING(cleaned_appointment_address FROM POSITION(',' IN cleaned_appointment_address) + 1))
		END AS address_line_2,
	postcode,
	LEFT(postcode, 2) AS postcode_prefix,
	county,
	country,
	urban_name
FROM cleaned;

DELETE
	FROM tmp_stage_5 USING tmp_stage_6
	WHERE tmp_stage_5.id = tmp_stage_6.id;

-- Copy over remaining address where first line and seond line were blank
INSERT INTO tmp_stage_6 (id,appointment_address,address_part_length,address_line_1,address_line_2,postcode,
						 postcode_prefix,county,country,urban_name)
SELECT id,
	   appointment_address,
	   address_part_length,
	   '',
	   NULL,
	   postcode,
	   postcode_prefix,
	   county,
	   country,
	   urban_name
FROM tmp_stage_5 l;

-- tmp table to derived from above processing to create table of postcode, urban_name, postcode_prefix AS postcode_prefix, county, country
CREATE
TEMP TABLE tmp_post_code_to_urban_name AS
SELECT postcode, max(urban_name) as urban_name, MAX(postcode_prefix) AS postcode_prefix, MAX(county) AS county, MAX(country) AS country
FROM tmp_stage_6
WHERE length(postcode) > 6
  AND urban_name IS NOT NULL
GROUP BY postcode;


-- Stage 7 -- finds addresses with matching postcodes and pulls in details if they exist from the above tmp_post_code_to_urban_name
-- Primarily for filling in missing urban_name and add reference

CREATE
TEMP TABLE tmp_stage_7 AS
SELECT DISTINCT id,
				gen_random_uuid() as reference,
				appointment_address,
				address_line_1,
				address_line_2,
				address_part_length,
				urban.urban_name,
				urban.postcode,
				urban.postcode_prefix,
				COALESCE(l.county,urban.county) AS county,
				COALESCE(l.country,urban.country) AS country
FROM tmp_stage_6 l
		 JOIN tmp_post_code_to_urban_name AS urban ON urban.postcode = l.postcode
WHERE l.urban_name IS NULL;

-- delete processed data from last source table
DELETE
FROM tmp_stage_6 USING tmp_stage_7
WHERE tmp_stage_6.id = tmp_stage_7.id;

-- adds un processed data
INSERT INTO tmp_stage_7 (id,reference,appointment_address,address_part_length,address_line_1,address_line_2,postcode,
						 postcode_prefix,county,country,urban_name)
SELECT id,
	   gen_random_uuid(),
	   appointment_address,
	   address_part_length,
	   address_line_1,
	   address_line_2,
	   postcode,
	   postcode_prefix,
	   county,
	   country,
	   urban_name
FROM tmp_stage_6 l;

DELETE
FROM tmp_stage_6 USING tmp_stage_7
WHERE tmp_stage_6.id = tmp_stage_7.id;

-- Stage 8 cleans the data and makes sure a null in the non nullable CVL data is now an empty string
-- also add's  source and created_timestamp and last_updated_timestamp
CREATE
TEMP TABLE tmp_stage_8 AS
SELECT  l.id as licence_id,
		reference,
		coalesce(case when length(address_line_1)=0 THEN address_line_2 else address_line_1 end,'')  as first_line,
		case when length(address_line_1)=0 THEN NULL else address_line_2 end as second_line,
		coalesce(urban_name,'') as town_or_city,
		coalesce(postcode,'') as postcode,
		county,
		country,
		'MANUAL' as source,
		coalesce(date_created,date_last_updated,CURRENT_TIMESTAMP) as created_timestamp,
		coalesce(date_last_updated,date_created,CURRENT_TIMESTAMP) as last_updated_timestamp
FROM tmp_stage_7 tmp
		 join licence l on tmp.id = l.id;


DROP TABLE IF EXISTS tmp_postcode_country;
DROP TABLE IF EXISTS tmp_uk_counties;
DROP TABLE IF EXISTS tmp_uk_urban_places;
DROP TABLE IF EXISTS tmp_common_addresses;
DROP TABLE IF EXISTS tmp_post_code_to_urban_name;
DROP TABLE IF EXISTS tmp_stage_1;
DROP TABLE IF EXISTS tmp_stage_2;
DROP TABLE IF EXISTS tmp_stage_3;
DROP TABLE IF EXISTS tmp_stage_4;
DROP TABLE IF EXISTS tmp_stage_5;
DROP TABLE IF EXISTS tmp_stage_6;
DROP TABLE IF EXISTS tmp_stage_7;


 /**
-- This will move the data over to the real DB but the flyway creation script needs to be run from the other ticket
-- https://dsdmoj.atlassian.net/browse/CVSL-353

ALTER TABLE address DISABLE TRIGGER set_address_last_updated_timestamp;

INSERT INTO address (id,reference, first_line, second_line, town_or_city, county, postcode,
   source, created_timestamp, last_updated_timestamp
) SELECT
	  licence_id,reference::text as reference, first_line, second_line, town_or_city, county, postcode,
	   source, created_timestamp, last_updated_timestamp
	FROM tmp_stage_8 order by licence_id;

INSERT INTO licence_appointment_address (licence_id, address_id)
	SELECT
	  tmp.licence_id,
	  a.id
	FROM tmp_stage_8 tmp
	JOIN address a ON a.reference = tmp.reference::text
    order by tmp.licence_id;

ALTER TABLE address ENABLE TRIGGER set_address_last_updated_timestamp;

DROP TABLE IF EXISTS tmp_stage_8;
*/
--
-- sql to allow us to check migrated data
--
--		SELECT l.id as licence_table_id,laa.licence_id,laa.address_id,a.id as address_table_ID,
--			l.appointment_address,
--			CONCAT_WS(', ',
--			NULLIF(TRIM(a.first_line), ''),
--			NULLIF(TRIM(a.second_line), ''),
--			NULLIF(TRIM(a.town_or_city), ''),
--			NULLIF(TRIM(a.county), ''),
--			NULLIF(TRIM(a.postcode), '')
--		  ) AS full_address_mirgated
--			FROM licence l
--			JOIN licence_appointment_address laa ON laa.licence_id = l.id
--			JOIN address a ON laa.address_id = a.id;
--
--   This is also useful
--   SELECT
--  a.first_line,
--  a.second_line,
--  a.town_or_city,
--  a.county,
--  a.postcode,
--  l.appointment_address,
--  CONCAT_WS(', ',
--    NULLIF(TRIM(a.first_line), ''),
--    NULLIF(TRIM(a.second_line), ''),
--    NULLIF(TRIM(a.town_or_city), ''),
--    NULLIF(TRIM(a.county), ''),
--    NULLIF(TRIM(a.postcode), '')
--  ) AS full_address_migrated
-- FROM licence l
-- JOIN licence_appointment_address laa ON laa.licence_id = l.id
-- JOIN address a ON laa.address_id = a.id
-- ORDER BY LENGTH(l.appointment_address);
--
-- put DESC at the end to get the long addresses...
--
-- Select
--	(Select count(*) from licence l) as licence_count,
--	(Select count(*) from licence l where l.appointment_address is not null) as licence_count_with_address,
--	(Select count(*) from address) as address_count,
--	(Select count(*) from licence_appointment_address laa ) as join_table_count;
--
-- SELECT a.first_line,a.second_line,a.town_or_city,a.county,a.postcode ,l.appointment_address
-- 		FROM address a
--		join licence l on l.id = a.id
--