-- This SQL tries to make sense out of the comma delimited string addresses found in the licence

PLEASE DO NOT RUN THIS YET!

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
				 WHEN full_postcode IS NOT NULL THEN SUBSTRING(cleaned FROM 1 FOR length(cleaned) - 3) || ' ' ||
													 SUBSTRING(cleaned FROM length(cleaned) - 2 FOR 3)
				 WHEN outward_code IS NOT NULL THEN outward_code
				 ELSE left_over END) AS postcode
FROM licence l
		 LEFT JOIN LATERAL (
	-- removes duplicate elements
	SELECT REPLACE(TRIM(string_agg(value,',' ORDER BY ord)),',,',',') AS VALUE
FROM (
	SELECT DISTINCT ON (TRIM (VALUE)) TRIM (VALUE) AS VALUE, ord FROM unnest(string_to_array(l.appointment_address, ',')) WITH ORDINALITY AS t(VALUE, ord) ) ) address_cleaned
ON TRUE
	LEFT JOIN LATERAL ( SELECT string_to_array(address_cleaned.value, ',') AS VALUE ) address_array ON TRUE
	LEFT JOIN LATERAL (
	SELECT (regexp_matches( regexp_replace(address_cleaned.value, '[^A-Za-z0-9 ,]', '', 'g'), '(([A-PR-UWYZ][0-9][0-9]?|[A-PR-UWYZ][A-HK-Y][0-9][0-9]?|[A-PR-UWYZ][0-9][A-HJKPSTUW]|[A-PR-UWYZ][A-HK-Y][0-9][ABEHMNPRVWXY])\s*[0-9][ABD-HJLNP-UW-Z]{2})', 'i' ))[1] AS full_postcode,
	(regexp_matches( regexp_replace(address_cleaned.value, '[^A-Za-z0-9 ,]', '', 'g'), '([A-PR-UWYZ][0-9][0-9]?|[A-PR-UWYZ][A-HK-Y][0-9][0-9]?|[A-PR-UWYZ][0-9][A-HJKPSTUW]|[A-PR-UWYZ][A-HK-Y][0-9][ABEHMNPRVWXY])', 'i' ))[1] AS outward_code,
	(regexp_matches(address_array.value[array_length(address_array.value, 1)], '^[A-Za-z](?=.*[0-9])(?=.*[A-Za-z0-9]).*'))[1] AS left_over )
	sub ON TRUE
	CROSS JOIN LATERAL ( SELECT regexp_replace(COALESCE (sub.full_postcode, ''), '[^A-Za-z0-9]', '', 'g') AS cleaned ) C
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
			 -- Wales-only
			 ('CF','Wales'),
			 ('LL','Wales'),
			 ('NP','Wales'),
			 ('SA','Wales'),
			 -- Northern Ireland-only
			 ('BT','Northern Ireland'),
			 -- England-only
			 ('AL','England'),
			 ('B','England'),
			 ('BA','England'),
			 ('BB','England'),
			 ('BD','England'),
			 ('BH','England'),
			 ('BL','England'),
			 ('BN','England'),
			 ('BR','England'),
			 ('BS','England'),
			 ('CB','England'),
			 ('CM','England'),
			 ('CO','England'),
			 ('CR','England'),
			 ('CT','England'),
			 ('CV','England'),
			 ('CW','England'),
			 ('DA','England'),
			 ('DE','England'),
			 ('DH','England'),
			 ('DL','England'),
			 ('DN','England'),
			 ('DT','England'),
			 ('DY','England'),
			 ('E','England'),
			 ('EC','England'),
			 ('EN','England'),
			 ('EX','England'),
			 ('FY','England'),
			 ('GL','England'),
			 ('GU','England'),
			 ('HA','England'),
			 ('HD','England'),
			 ('HG','England'),
			 ('HP','England'),
			 ('HU','England'),
			 ('HX','England'),
			 ('IG','England'),
			 ('IP','England'),
			 ('KT','England'),
			 ('L','England'),
			 ('LA','England'),
			 ('LE','England'),
			 ('LN','England'),
			 ('LS','England'),
			 ('LU','England'),
			 ('M','England'),
			 ('ME','England'),
			 ('MK','England'),
			 ('N','England'),
			 ('NE','England'),
			 ('NG','England'),
			 ('NN','England'),
			 ('NR','England'),
			 ('NW','England'),
			 ('OL','England'),
			 ('OX','England'),
			 ('PE','England'),
			 ('PL','England'),
			 ('PO','England'),
			 ('PR','England'),
			 ('RG','England'),
			 ('RH','England'),
			 ('RM','England'),
			 ('S','England'),
			 ('SE','England'),
			 ('SG','England'),
			 ('SK','England'),
			 ('SL','England'),
			 ('SM','England'),
			 ('SN','England'),
			 ('SO','England'),
			 ('SP','England'),
			 ('SR','England'),
			 ('SS','England'),
			 ('ST','England'),
			 ('SW','England'),
			 ('TA','England'),
			 ('TF','England'),
			 ('TN','England'),
			 ('TQ','England'),
			 ('TR','England'),
			 ('TS','England'),
			 ('TW','England'),
			 ('UB','England'),
			 ('W','England'),
			 ('WA','England'),
			 ('WC','England'),
			 ('WD','England'),
			 ('WF','England'),
			 ('WN','England'),
			 ('WR','England'),
			 ('WS','England'),
			 ('WV','England'),
			 ('YO','England'),
			 -- Scotland-only
			 ('AB','Scotland'),
			 ('DD','Scotland'),
			 ('DG','Scotland'),
			 ('EH','Scotland'),
			 ('FK','Scotland'),
			 ('G','Scotland'),
			 ('HS','Scotland'),
			 ('IV','Scotland'),
			 ('KA','Scotland'),
			 ('KW','Scotland'),
			 ('KY','Scotland'),
			 ('ML','Scotland'),
			 ('PA','Scotland'),
			 ('PH','Scotland'),
			 ('ZE','Scotland')) AS t(postcode_prefix,country);


-- Stage 2 Adds post code prefix and country based on post code

CREATE
TEMP TABLE tmp_stage_2 AS
SELECT address.*, country.postcode_prefix, country.country
FROM tmp_stage_1 address
		 LEFT JOIN tmp_postcode_country AS country ON POSITION(country.postcode_prefix IN address.postcode) = 1
WHERE LENGTH(country.postcode_prefix) = 2;

-- delete processed data from last source table
DELETE
FROM tmp_stage_1 USING tmp_stage_2
WHERE tmp_stage_1.id = tmp_stage_2.id;


INSERT INTO tmp_stage_2 (id,appointment_address,address_part_length,postcode,postcode_prefix,country)
SELECT address.*, country.postcode_prefix, country.country
FROM tmp_stage_1 address
		 LEFT JOIN tmp_postcode_country AS country ON POSITION(country.postcode_prefix IN address.postcode) = 1
WHERE LENGTH(country.postcode_prefix) = 1;

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
VALUES ('England','Kent'),
	   ('England','Cheshire'),
	   ('England','Durham'),
	   ('England','Surrey'),
	   ('England','Cornwall'),
	   ('England','Yorkshire'),
	   ('England','County Durham'),
	   ('England','Tyne and Wear'),
	   ('England','Middlesex'),
	   ('England','Worcestershire'),
	   ('England','City of London'),
	   ('England','Lancashire'),
	   ('England','Sussex'),
	   ('England','South Yorkshire'),
	   ('England','Norfolk'),
	   ('England','Herefordshire'),
	   ('England','East Riding of Yorkshire'),
	   ('England','East Yorkshire'), -- offically know as above
	   ('England','Hertfordshire'),
	   ('England','Lincolnshire'),
	   ('England','West Yorkshire'),
	   ('England','Warwickshire'),
	   ('England','Staffordshire'),
	   ('England','Somerset'),
	   ('England','Bristol'),
	   ('England','Cumbria'),
	   ('England','Hampshire'),
	   ('England','Northamptonshire'),
	   ('England','Westmorland'),
	   ('England','Greater Manchester'),
	   ('England','Devon'),
	   ('England','Merseyside'),
	   ('England','Cambridgeshire'),
	   ('England','Gloucestershire'),
	   ('England','Essex'),
	   ('England','Huntingdonshire'),
	   ('England','Dorset'),
	   ('England','Northumberland'),
	   ('England','Isle of Wight'),
	   ('England','Greater London'),
	   ('England','Buckinghamshire'),
	   ('England','Rutland'),
	   ('England','Berkshire'),
	   ('England','Leicestershire'),
	   ('England','East Sussex'),
	   ('England','Suffolk'),
	   ('England','Wiltshire'),
	   ('England','West Sussex'),
	   ('England','Derbyshire'),
	   ('England','Bedfordshire'),
	   ('England','North Yorkshire'),
	   ('England','Oxfordshire'),
	   ('England','Nottinghamshire'),
	   ('England','Shropshire'),
	   ('England','West Midlands'),
	   ('England','Cumberland'),
	   ('Northern Ireland','Lisburn and Castlereagh'),
	   ('Northern Ireland','Derry and Strabane'),
	   ('Northern Ireland','Belfast'),
	   ('Northern Ireland','Tyrone'),
	   ('Northern Ireland','Londonderry'),
	   ('Northern Ireland','Armagh'),
	   ('Northern Ireland','Antrim and Newtownabbey'),
	   ('Northern Ireland','Fermanagh and Omagh'),
	   ('Northern Ireland','Down'),
	   ('Northern Ireland','Armagh, Banbridge and Craigavon'),
	   ('Northern Ireland','Fermanagh'),
	   ('Northern Ireland','Causeway Coast and Glens'),
	   ('Northern Ireland','Antrim'),
	   ('Northern Ireland','Newry, Mourne and Down'),
	   ('Northern Ireland','Mid Ulster'),
	   ('Northern Ireland','Mid and East Antrim'),
	   ('Scotland','Kirkcudbrightshire'),
	   ('Scotland','Caithness'),
	   ('Scotland','Ross-shire'),
	   ('Scotland','Kincardineshire'),
	   ('Scotland','Dumfriesshire'),
	   ('Scotland','Orkney Islands'),
	   ('Scotland','Fife'),
	   ('Scotland','Peeblesshire'),
	   ('Scotland','West Lothian'),
	   ('Scotland','Sutherland'),
	   ('Scotland','Dunbartonshire'),
	   ('Scotland','East Ayrshire'),
	   ('Scotland','Argyll and Bute'),
	   ('Scotland','Banffshire'),
	   ('Scotland','South Lanarkshire'),
	   ('Scotland','Roxburghshire'),
	   ('Scotland','Lanarkshire'),
	   ('Scotland','Moray'),
	   ('Scotland','Glasgow City'),
	   ('Scotland','Nairnshire'),
	   ('Scotland','Inverness-shire'),
	   ('Scotland','Clackmannanshire'),
	   ('Scotland','Stirling'),
	   ('Scotland','Dumfries and Galloway'),
	   ('Scotland','Aberdeen City'),
	   ('Scotland','Perthshire'),
	   ('Scotland','East Dunbartonshire'),
	   ('Scotland','North Lanarkshire'),
	   ('Scotland','Wigtownshire'),
	   ('Scotland','Angus'),
	   ('Scotland','Inverclyde'),
	   ('Scotland','Midlothian'),
	   ('Scotland','West Dunbartonshire'),
	   ('Scotland','Argyll'),
	   ('Scotland','Kinross-shire'),
	   ('Scotland','Buteshire'),
	   ('Scotland','Falkirk'),
	   ('Scotland','East Renfrewshire'),
	   ('Scotland','Perth and Kinross'),
	   ('Scotland','Aberdeenshire'),
	   ('Scotland','East Lothian'),
	   ('Scotland','Ayrshire'),
	   ('Scotland','Western Isles'),
	   ('Scotland','South Ayrshire'),
	   ('Scotland','Highland'),
	   ('Scotland','Shetland Islands'),
	   ('Scotland','Orkney'),
	   ('Scotland','Shetland'),
	   ('Scotland','Berwickshire'),
	   ('Scotland','Scottish Borders'),
	   ('Scotland','Dundee City'),
	   ('Scotland','Selkirkshire'),
	   ('Scotland','City of Edinburgh'),
	   ('Scotland','Renfrewshire'),
	   ('Scotland','Stirlingshire'),
	   ('Scotland','North Ayrshire'),
	   ('Wales','Powys'),
	   ('Wales','Glamorgan'),
	   ('Wales','Denbighshire'),
	   ('Wales','Cardiff'),
	   ('Wales','Carmarthenshire'),
	   ('Conwy','Conwy'),
	   ('Wales','Flintshire'),
	   ('Wales','Swansea'),
	   ('Wales','Gwent'),
	   ('Wales','Anglesey'),
	   ('Wales','Dyfed'),
	   ('Wales','Merionethshire'),
	   ('Wales','Cardiganshire'),
	   ('Wales','South Glamorgan'),
	   ('Wales','Mid Glamorgan'),
	   ('Wales','Clwyd'),
	   ('Wales','Radnorshire'),
	   ('Wales','Monmouthshire'),
	   ('Wales','West Glamorgan'),
	   ('Wales','Caernarfonshire'),
	   ('Wales','Vale of Glamorgan'),
	   ('Wales','Brecknockshire'),
	   ('Wales','Gwynedd'),
	   ('Wales','Montgomeryshire'),
	   ('Wales','Pembrokeshire');

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
  country TEXT NOT NULL,     -- England, Scotland, Wales, Northern Ireland
  county TEXT NOT NULL,       -- Ceremonial / administrative county
  urban_postcode_prefix text NULL
);

INSERT INTO tmp_uk_urban_places (urban_name,place_type,country,county,urban_postcode_prefix)
VALUES
-- Cities (alphabetical)
('Bath','City','England','Somerset','BA'),
('Belfast','City','Northern Ireland','County Antrim','BT'),
('Birmingham','City','England','West Midlands','B'),
('Bradford','City','England','West Yorkshire','BD'),
('Brighton and Hove','City','England','East Sussex','BN'),
('Bristol','City','England','Bristol','BS'),
('Cambridge','City','England','Cambridgeshire','CB'),
('Canterbury','City','England','Kent','CT'),
('Cardiff','City','Wales','Cardiff','CF'),
('Carlisle','City','England','Cumbria','CA'),
('Chelmsford','City','England','Essex','CM'),
('Chester','City','England','Cheshire','CH'),
('Chichester','City','England','West Sussex','PO'),
('Coventry','City','England','West Midlands','CV'),
('Derby','City','England','Derbyshire','DE'),
('Derry','City','Northern Ireland','County Londonderry','BT'),
('Durham','City','England','County Durham','DH'),
('Ely','City','England','Cambridgeshire','CB'),
('Edinburgh','City','Scotland','City of Edinburgh','EH'),
('Exeter','City','England','Devon','EX'),
('Glasgow','City','Scotland','Glasgow City','G'),
('Gloucester','City','England','Gloucestershire','GL'),
('Hull','City','England','East Riding of Yorkshire','HU'),
('Inverness','City','Scotland','Highland','IV'),
('Kingston upon Hull','City','England','East Riding of Yorkshire','HU'),
('Lancaster','City','England','Lancashire','LA'),
('Leeds','City','England','West Yorkshire','LS'),
('Leicester','City','England','Leicestershire','LE'),
('Lichfield','City','England','Staffordshire','WS'),
('Lincoln','City','England','Lincolnshire','LN'),
('Lisburn','City','Northern Ireland','County Antrim','BT'),
('Liverpool','City','England','Merseyside','L'),
('London','City','England','Greater London',NULL), -- Multiple prefixes
('Manchester','City','England','Greater Manchester','M'),
('Newcastle upon Tyne','City','England','Tyne and Wear','NE'),
('Newport','City','Wales','Newport','NP'),
('Norwich','City','England','Norfolk','NR'),
('Nottingham','City','England','Nottinghamshire','NG'),
('Oxford','City','England','Oxfordshire','OX'),
('Peterborough','City','England','Cambridgeshire','PE'),
('Perth','City','Scotland','Perth and Kinross','PH'),
('Plymouth','City','England','Devon','PL'),
('Portsmouth','City','England','Hampshire','PO'),
('Preston','City','England','Lancashire','PR'),
('Ripon','City','England','North Yorkshire','HG'),
('Salford','City','England','Greater Manchester','M'),
('Salisbury','City','England','Wiltshire','SP'),
('Sheffield','City','England','South Yorkshire','S'),
('Southampton','City','England','Hampshire','SO'),
('St Albans','City','England','Hertfordshire','AL'),
('St Andrews','City','Scotland','Fife','KY'),
('St Davids','City','Wales','Pembrokeshire','SA'),
('Stirling','City','Scotland','Stirling','FK'),
('Swansea','City','Wales','Swansea','SA'),
('Truro','City','England','Cornwall','TR'),
('Wakefield','City','England','West Yorkshire','WF'),
('Wells','City','England','Somerset','BA'),
('Winchester','City','England','Hampshire','SO'),
('Wolverhampton','City','England','West Midlands','WV'),
('Worcester','City','England','Worcestershire','WR'),
('York','City','England','North Yorkshire','YO'),

-- Towns (alphabetical)
('Abingdon on Thames','Town','England','Oxfordshire','OX'),
('Aberdare','Town','Wales','Rhondda Cynon Taf','CF'),
('Accrington','Town','England','Lancashire','BB'),
('Alexandria','Town','Scotland','West Dunbartonshire','G'),
('Alfreton','Town','England','Derbyshire','DE'),
('Antrim','Town','Northern Ireland','County Antrim','BT'),
('Andover','Town','England','Hampshire','SP'),
('Armagh','City','Northern Ireland','County Armagh','BT'),
('Ashford','Town','England','Kent','TN'),
('Aylesbury','Town','England','Buckinghamshire','HP'),
('Banbury','Town','England','Oxfordshire','OX'),
('Bangor','City','Wales','Gwynedd','LL'),
-- ('Bangor', 'Town', 'Northern Ireland', 'County Down', 'BT'),
('Barnsley','Town','England','South Yorkshire','S'),
('Barnstaple','Town','England','Devon','EX'),
('Barrow in Furness','Town','England','Cumbria','LA'),
('Barrhead','Town','Scotland','East Renfrewshire','G'),
('Barry','Town','Wales','Vale of Glamorgan','CF'),
('Basildon','Town','England','Essex','SS'),
('Basingstoke','Town','England','Hampshire','RG'),
('Bathgate','Town','Scotland','West Lothian','EH'),
('Bellshill','Town','Scotland','North Lanarkshire','ML'),
('Bingley','Town','England','West Yorkshire','BD'),
('Birkenhead','Town','England','Merseyside','CH'),
('Blackburn','Town','England','Lancashire','BB'),
('Blackpool','Town','England','Lancashire','FY'),
('Blyth','Town','England','Northumberland','NE'),
('Bognor Regis','Town','England','West Sussex','PO'),
('Boston','Town','England','Lincolnshire','PE'),
('Bournemouth','Town','England','Dorset','BH'),
('Brighton','Town','England','East Sussex','BN'),
('Broxburn','Town','Scotland','West Lothian','EH'),
('Bromsgrove','Town','England','Worcestershire','B'),
('Burnley','Town','England','Lancashire','BB'),
('Burton upon Trent','Town','England','Staffordshire','DE'),
('Bury','Town','England','Greater Manchester','BL'),
('Bolton','Town','England','Greater Manchester','BL'),
('Caerphilly','Town','Wales','Caerphilly','CF'),
('Calne','Town','England','Wiltshire','SN'),
('Camborne','Town','England','Cornwall','TR'),
('Cannock','Town','England','Staffordshire','WS'),
('Carrickfergus','Town','Northern Ireland','County Antrim','BT'),
('Castleford','Town','England','West Yorkshire','WF'),
('Chesterfield','Town','England','Derbyshire','S'),
('Chippenham','Town','England','Wiltshire','SN'),
('Clacton on Sea','Town','England','Essex','CO'),
('Coatbridge','Town','Scotland','North Lanarkshire','ML'),
('Colchester','Town','England','Essex','CO'),
('Coleraine','Town','Northern Ireland','County Londonderry','BT'),
('Consett','Town','England','County Durham','DH'),
('Corby','Town','England','Northamptonshire','NN'),
('Crawley','Town','England','West Sussex','RH'),
('Crewe','Town','England','Cheshire','CW'),
('Croydon','Town','England','Greater London','CR'),
('Cwmbran','Town','Wales','Torfaen','NP'),
('Darlington','Town','England','County Durham','DL'),
('Dewsbury','Town','England','West Yorkshire','WF'),
('Doncaster','Town','England','South Yorkshire','DN'),
('Downpatrick','City','Northern Ireland','County Down','BT'),
('Dover','Town','England','Kent','CT'),
('Dumbarton','Town','Scotland','West Dunbartonshire','G'),
('Dumfries','Town','Scotland','Dumfries and Galloway','DG'),
('Eastbourne','Town','England','East Sussex','BN'),
('Elgin','Town','Scotland','Moray','IV'),
('Ellesmere Port','Town','England','Cheshire','CH'),
('Exmouth','Town','England','Devon','EX'),
('Farnborough','Town','England','Hampshire','GU'),
('Felixstowe','Town','England','Suffolk','IP'),
('Guildford','Town','England','Surrey','GU'),
('Gateshead','Town','England','Tyne and Wear','NE'),
('Halifax','Town','England','West Yorkshire','HX'),
('Harlow','Town','England','Essex','CM'),
('Hartlepool','Town','England','County Durham','TS'),
('Hereford','City','England','Herefordshire','HR'),
('High Wycombe','Town','England','Buckinghamshire','HP'),
('Huddersfield','Town','England','West Yorkshire','HD'),
('Ipswich','Town','England','Suffolk','IP'),
('Kingston upon Thames','Town','England','Greater London','KT'),
('Kirkintilloch','Town','Scotland','East Dunbartonshire','G'),
('Larne','Town','Northern Ireland','County Antrim','BT'),
('Llanelli','Town','Wales','Carmarthenshire','SA'),
('Londonderry','Town','Northern Ireland','County Londonderry','BT'),
('Maidstone','Town','England','Kent','ME'),
('Mansfield','Town','England','Nottinghamshire','NG'),
('Medway Towns','Town','England','Kent','ME'),
('Merthyr Tydfil','Town','Wales','Merthyr Tydfil','CF'),
('Middlesbrough','Town','England','North Yorkshire','TS'),
('Milton Keynes','Town','England','Buckinghamshire','MK'),
('Morecambe','Town','England','Lancashire','LA'),
('Musselburgh','Town','Scotland','East Lothian','EH'),
('Neath','Town','Wales','Neath Port Talbot','SA'),
('Newry','City','Northern Ireland','County Down','BT'),
('Newtownabbey','Town','Northern Ireland','County Antrim','BT'),
('Oldham','Town','England','Greater Manchester','OL'),
('Penicuik','Town','Scotland','Midlothian','EH'),
('Peterhead','Town','Scotland','Aberdeenshire','AB'),
('Poole','Town','England','Dorset','BH'),
('Pontypridd','Town','Wales','Rhondda Cynon Taf','CF'),
('Reading','Town','England','Berkshire','RG'),
('Renfrew','Town','Scotland','Renfrewshire','PA'),
('Rochdale','Town','England','Greater Manchester','OL'),
('Rotherham','Town','England','South Yorkshire','S'),
('Saint Helens','Town','England','Merseyside','WA'),
('Sale','Town','England','Greater Manchester','M'),
('Scarborough','Town','England','North Yorkshire','YO'),
('Slough','Town','England','Berkshire','SL'),
('Solihull','Town','England','West Midlands','B'),
('Southend on Sea','Town','England','Essex','SS'),
('St Asaph','City','Wales','Denbighshire','LL'),
('Stoke on Trent','Town','England','Staffordshire','ST'),
('Stockport','Town','England','Greater Manchester','SK'),
('Sunderland','Town','England','Tyne and Wear','SR'),
('Swindon','Town','England','Wiltshire','SN'),
('Telford','Town','England','Shropshire','TF'),
('Uddingston','Town','Scotland','South Lanarkshire','G'),
('Warrington','Town','England','Cheshire','WA'),
('Wigan','Town','England','Greater Manchester','WN'),
('Wishaw','Town','Scotland','North Lanarkshire','ML'),
('Wrexham','Town','Wales','Wrexham','LL'),
('Richmond','Town','England','Greater London','TW'),
('Hornchurch','Town','England','Greater London','RM'),
('Northampton','Town','England','Northamptonshire','NN'),
('West Bromwich','Town','England','West Midland','B'),
('Walsall','Town','England','West Midland','WS'),
('Luton','Town','England','Bedfordshire','LU'),
('Atherton','Town','England','Greater Manchester','M'),
('Scunthorpe','Town','England','North Lincolnshire','DN'),
('Nuneaton','Town','England','Warwickshire','CV'),
('Lewisham','Town','England','Greater London','SE'),
('Torquay','Town','England','Devon','TQ'),
('Prescot','Town','England','Merseyside','L'),
('Aldershot','Town','England','Hampshire','GU'),
('Aberdeen','City','Scotland','Aberdeen City','AB'),
('Dundee','City','Scotland','Dundee City','DD'),
('Watford','Town','England','Hertfordshire','WD'),
('Stafford','Town','England','Staffordshire','ST'),
('Conwy','Town','Wales','Conwy','LL'),
('Chatham','Town','England','Kent','ME'),
('Stockton','Town','England','County Durham','TS'),
('Laindon','Town','England','Essex','SS'),
('Folkestone','Town','England','Kent','CT'),
('Shrewsbury','Town','England','Shropshire','SY'),
('Haverfordwest','Town','Wales','Pembrokeshire','SA'),
('Bridgend','Town','Wales','Bridgend','CF'),
('Newport (Isle of Wight)','Town','England','Isle of Wight','PO'),
('Royal Leamington Spa','Town','England','Warwickshire','CV'),
('Redditch','Town','England','Worcestershire','B'),
('Kettering','Town','England','Northamptonshire','NN'),
('Grimsby','Town','England','Lincolnshire','DN'),
('Harrogate','Town','England','North Yorkshire','HG'),
('Skegness','Town','England','Lincolnshire','PE'),
('Rugby','Town','England','Warwickshire','CV'),
('King’s Lynn','Town','England','Norfolk','PE'),
('Worksop','Town','England','Nottinghamshire','S'),
('Bishop Auckland','Town','England','County Durham','DL'),
('Hastings','Town','England','East Sussex','TN'),
('Morpeth','Town','England','Northumberland','NE'),
('Bracknell','Town','England','Berkshire','RG'),
('Banbridge','Town','Northern Ireland','County Down','BT'),
('Motherwell','Town','Scotland','North Lanarkshire','ML'),
('Ayr','Town','Scotland','South Ayrshire','KA'),
('Loughborough','Town','England','Leicestershire','LE'),
('Greenock','Town','Scotland','Inverclyde','PA'),
('Alnwick','Town','England','Northumberland','NE'),
('Arbroath','Town','Scotland','Angus','DD'),
('Ashington','Town','England','Northumberland','NE'),
('Beccles','Town','England','Suffolk','NR'),
('Beverley','Town','England','East Riding of Yorkshire','HU'),
('Bideford','Town','England','Devon','EX'),
('Biggleswade','Town','England','Bedfordshire','SG'),
('Bishop’s Stortford','Town','England','Hertfordshire','CM'),
('Blaydon','Town','England','Tyne and Wear','NE'),
('Bridport','Town','England','Dorset','DT'),
('Buckfastleigh','Town','England','Devon','TQ'),
('Burntisland','Town','Scotland','Fife','KY'),
('Caithness','Town','Scotland','Highland','KW'),
('Carnoustie','Town','Scotland','Angus','DD'),
('Castlereagh','Town','Northern Ireland','County Down','BT'),
('Chipping Norton','Town','England','Oxfordshire','OX'),
('Coalville','Town','England','Leicestershire','LE'),
('Cowbridge','Town','Wales','Vale of Glamorgan','CF'),
('Cromer','Town','England','Norfolk','NR'),
('Cupar','Town','Scotland','Fife','KY'),
('Dalkeith','Town','Scotland','Midlothian','EH'),
('Dunbar','Town','Scotland','East Lothian','EH'),
('Dunfermline','Town','Scotland','Fife','KY'),
('Dunkeld','Town','Scotland','Perth and Kinross','PH'),
('Easingwold','Town','England','North Yorkshire','YO'),
('Epsom','Town','England','Surrey','KT'),
('Fakenham','Town','England','Norfolk','NR'),
('Filey','Town','England','North Yorkshire','YO'),
('Forfar','Town','Scotland','Angus','DD'),
('Fort William','Town','Scotland','Highland','PH'),
('Galashiels','Town','Scotland','Scottish Borders','TD'),
('Gosport','Town','England','Hampshire','PO'),
('Grangemouth','Town','Scotland','Falkirk','FK'),
('Havant','Town','England','Hampshire','PO'),
('Helensburgh','Town','Scotland','Argyll and Bute','G'),
('Holyhead','Town','Wales','Anglesey','LL'),
('Huntingdon','Town','England','Cambridgeshire','PE'),
('Inverurie','Town','Scotland','Aberdeenshire','AB'),
('Jedburgh','Town','Scotland','Scottish Borders','TD'),
('Keighley','Town','England','West Yorkshire','BD'),
('Kendal','Town','England','Cumbria','LA'),
('Kelso','Town','Scotland','Scottish Borders','TD'),
('Kilwinning','Town','Scotland','North Ayrshire','KA'),
('Knaresborough','Town','England','North Yorkshire','HG'),
('Lanark','Town','Scotland','South Lanarkshire','ML'),
('Liskeard','Town','England','Cornwall','PL'),
('Lossiemouth','Town','Scotland','Moray','IV'),
('Louth','Town','England','Lincolnshire','LN'),
('Maldon','Town','England','Essex','CM'),
('Melrose','Town','Scotland','Scottish Borders','TD'),
('Moffat','Town','Scotland','Dumfries and Galloway','DG'),
('Montrose','Town','Scotland','Angus','DD'),
('Moreton-in-Marsh','Town','England','Gloucestershire','GL'),
('Nairn','Town','Scotland','Highland','IV'),
('Nantwich','Town','England','Cheshire','CW'),
('Newark-on-Trent','Town','England','Nottinghamshire','NG'),
('Newburgh','Town','Scotland','Fife','KY'),
('Newcastle Emlyn','Town','Wales','Ceredigion','SA'),
('Northallerton','Town','England','North Yorkshire','DL'),
('Northwich','Town','England','Cheshire','CW'),
('Oban','Town','Scotland','Argyll and Bute','PA'),
('Okehampton','Town','England','Devon','EX'),
('Old Kilpatrick','Town','Scotland','West Dunbartonshire','G'),
('Ormskirk','Town','England','Merseyside','L'),
('Peebles','Town','Scotland','Scottish Borders','EH'),
('Penrith','Town','England','Cumbria','CA'),
('Penzance','Town','England','Cornwall','TR'),
('Pitlochry','Town','Scotland','Perth and Kinross','PH'),
('Prestwick','Town','Scotland','South Ayrshire','KA'),
('Redruth','Town','England','Cornwall','TR'),
('Rothbury','Town','England','Northumberland','NE'),
('Saltburn-by-the-Sea','Town','England','North Yorkshire','TS'),
('Sittingbourne','Town','England','Kent','ME'),
('Skelmersdale','Town','England','Lancashire','WN'),
('South Molton','Town','England','Devon','EX'),
('South Shields','Town','England','Tyne and Wear','NE'),
('Stonehaven','Town','Scotland','Aberdeenshire','AB'),
('Stornoway','Town','Scotland','Western Isles','HS'),
('Stowmarket','Town','England','Suffolk','IP'),
('Stranraer','Town','Scotland','Dumfries and Galloway','DG'),
('Sutton Coldfield','Town','England','West Midlands','B'),
('Tavistock','Town','England','Devon','PL'),
('Tenterden','Town','England','Kent','TN'),
('Thurso','Town','Scotland','Highland','KW'),
('Tonbridge','Town','England','Kent','TN'),
('Troon','Town','Scotland','South Ayrshire','KA'),
('Urmston','Town','England','Greater Manchester','M'),
('Uttoxeter','Town','England','Staffordshire','ST'),
('West Kirby','Town','England','Merseyside','CH'),
('Wetherby','Town','England','West Yorkshire','LS'),
('Whitby','Town','England','North Yorkshire','YO'),
('Wick','Town','Scotland','Highland','KW'),
('Wigtown','Town','Scotland','Dumfries and Galloway','DG'),
('Witney','Town','England','Oxfordshire','OX'),
('Worsborough','Town','England','South Yorkshire','S'),
('Acton','Town','England','Greater London','W'),
('Ashton-under-Lyne','Town','England','Greater Manchester','OL'),
('Bexleyheath','Town','England','Greater London','DA'),
('Bootle','Town','England','Merseyside','L'),
('Buxton','Town','England','Derbyshire','SK'),
('Caernarfon','Town','Wales','Gwynedd','LL'),
('Dudley','Town','England','West Midlands','DY'),
('Eccles','Town','England','Greater Manchester','M'),
('Huyton','Town','England','Merseyside','L'),
('Ilford','Town','England','Greater London','IG'),
('Kidderminster','Town','England','Worcestershire','DY'),
('Leytonstone','Town','England','Greater London','E'),
('Lowestoft','Town','England','Suffolk','NR'),
('Newcastle','City','England','Tyne and Wear','NE'),
('Oldbury','Town','England','West Midlands','B'),
('Orpington','Town','England','Kent','BR'),
('Peterlee','Town','England','County Durham','SR'),
('Purbrook','Town','England','Hampshire','PO'),
('Ramsgate','Town','England','Kent','CT'),
('St Leonards on Sea','Town','England','East Sussex','TN'),
('Staines','Town','England','Surrey','TW'),
('Tamworth','Town','England','Staffordshire','B'),
('Taunton','Town','England','Somerset','TA'),
('Tunbridge Wells','Town','England','Kent','TN'),
('Uxbridge','Town','England','Greater London','UB'),
('Waterlooville','Town','England','Hampshire','PO'),
('Weymouth','Town','England','Dorset','DT'),
('Workington','Town','England','Cumbria','CA'),
('Yeovil','Town','England','Somerset','BA'),
('Leamington Spa','Town','England','Warwickshire','CV'),
('Stevenage','Town','England','Hertfordshire','SG'),
('Wallsend','Town','England','Tyne and Wear','NE');


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
CREATE
TEMP TABLE tmp_stage_6 AS
SELECT id,
	   appointment_address,
	   address_part_length,
	   TRIM(SPLIT_PART(cleaned.cleaned_appointment_address,',',1)) AS address_line_1,
	   NULLIF(TRIM(SUBSTRING(cleaned.cleaned_appointment_address FROM
							 POSITION(',' IN cleaned.cleaned_appointment_address) + 1)),'') AS address_line_2,
	   l.postcode,
	   l.postcode_prefix,
	   l.county,
	   l.country,
	   l.urban_name
FROM tmp_stage_5 l
		 JOIN LATERAL ( SELECT string_to_array(l.appointment_address,',') AS address_array,
							   array_length(string_to_array(l.appointment_address,','),1) AS a_len,
							   LOWER(l.county) AS county,
							   LOWER(l.urban_name) AS urban_name,
							   LOWER(l.country) AS country,
							   REPLACE(LOWER(l.postcode),' ','') AS postcode ) ref ON TRUE
		 JOIN LATERAL ( SELECT LOWER(TRIM(ref.address_array[ref.a_len])) AS part1,
							   LOWER(TRIM(ref.address_array[ref.a_len - 1])) AS part2,
							   LOWER(TRIM(ref.address_array[ref.a_len - 2])) AS part3,
							   LOWER(TRIM(ref.address_array[ref.a_len - 3])) AS part4 ) parts ON TRUE
		 LEFT JOIN LATERAL ( SELECT CASE
										WHEN REPLACE(parts.part1,' ','') = ref.postcode OR
											 parts.part1 IN (ref.county,ref.urban_name,ref.country) THEN REGEXP_REPLACE(
												l.appointment_address,CONCAT(' *,? *',ref.address_array[ref.a_len]),'',
												'gi')
										ELSE l.appointment_address END AS step1 ) s1 ON TRUE
		 LEFT JOIN LATERAL ( SELECT CASE
										WHEN REPLACE(parts.part2,' ','') = ref.postcode OR
											 parts.part2 IN (ref.county,ref.urban_name,ref.country) THEN REGEXP_REPLACE(
												s1.step1,CONCAT(' *,? *',ref.address_array[ref.a_len - 1]),'','gi')
										ELSE s1.step1 END AS step2 ) s2 ON TRUE
		 LEFT JOIN LATERAL ( SELECT CASE
										WHEN REPLACE(parts.part3,' ','') = ref.postcode OR
											 parts.part3 IN (ref.county,ref.urban_name,ref.country) THEN REGEXP_REPLACE(
												s2.step2,CONCAT(' *,? *',ref.address_array[ref.a_len - 2]),'','gi')
										ELSE s2.step2 END AS step3 ) s3 ON TRUE
		 LEFT JOIN LATERAL ( SELECT CASE
										WHEN REPLACE(parts.part4,' ','') = ref.postcode OR
											 parts.part4 IN (ref.county,ref.urban_name,ref.country) THEN REGEXP_REPLACE(
												s3.step3,CONCAT(' *,? *',ref.address_array[ref.a_len - 3]),'','gi')
										ELSE s3.step3 END AS raw_address ) s4 ON TRUE
		 LEFT JOIN LATERAL ( SELECT REGEXP_REPLACE(s4.raw_address,',\s*$','','gi') AS cleaned_appointment_address ) cleaned
				   ON TRUE
WHERE l.complete IS FALSE;

-- delete processed data from last source table
DELETE
FROM tmp_stage_5 USING tmp_stage_6
WHERE tmp_stage_5.id = tmp_stage_6.id;

-- adds un processed data
INSERT INTO tmp_stage_6 (id,appointment_address,address_part_length,address_line_1,address_line_2,postcode,
						 postcode_prefix,county,country,urban_name)
SELECT id,
	   appointment_address,
	   address_part_length,
	   address_line_1,
	   address_line_2,
	   postcode,
	   postcode_prefix,
	   county,
	   country,
	   urban_name
FROM tmp_stage_5 l;

DELETE
FROM tmp_stage_5 USING tmp_stage_6
WHERE tmp_stage_5.id = tmp_stage_6.id;


-- check to see if the correct count
select (select count(*) as processed from tmp_stage_6),(select count(*) as orgininal from licence where appointment_address is not null );

-- tmp table to derived from above processing to create table of postcode, urban_name, postcode_prefix AS postcode_prefix, county, country
CREATE
TEMP TABLE tmp_post_code_to_urban_name AS
SELECT postcode, max(urban_name) as urban_name, MAX(postcode_prefix) AS postcode_prefix, MAX(county) AS county, MAX(country) AS country
FROM tmp_stage_6
WHERE length(postcode) > 6
  AND urban_name IS NOT NULL
GROUP BY postcode;


-- Stage 7 -- finds addresses with matching postcodes and pulls in details if they exist from the above tmp_post_code_to_urban_name
-- Primarily for filling in missing urban_name

CREATE
TEMP TABLE tmp_stage_7 AS
SELECT DISTINCT id,
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
INSERT INTO tmp_stage_7 (id,appointment_address,address_part_length,address_line_1,address_line_2,postcode,
						 postcode_prefix,county,country,urban_name)
SELECT id,
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

/*
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
*/
-- DROP TABLE IF EXISTS tmp_stage_7;
-- query to match the result with the inital data
-- select (select count(*) as processed from tmp_stage_7),(select count(*) as orgininal from licence where appointment_address is not null );
