INSERT INTO customer (cif, first_name, last_name, email, phone, ssn_last4, segment, kyc_status, customer_since) VALUES
  ('100000001', 'Dana',   'Whitfield', 'dana.whitfield@example.com',   '+12125550143', '4417', 'PREFERRED', 'VERIFIED', '2014-03-11'),
  ('100000002', 'Marcus', 'Oyelaran',  'marcus.oyelaran@example.com',  '+13475550198', '8820', 'RETAIL',    'VERIFIED', '2019-08-02'),
  ('100000003', 'Priya',  'Raghunath', 'priya.raghunath@example.com',  '+19175550177', '2051', 'PREMIER',   'VERIFIED', '2008-01-24'),
  ('100000004', 'Theresa','Okafor',    'theresa.okafor@example.com',   '+17185550122', '6639', 'RETAIL',    'REVIEW',   '2022-11-15');

INSERT INTO product_limit (product_code, product_name, per_transfer_limit) VALUES
  ('DDA-CHK', 'Advantage Checking',           10000.00),
  ('DDA-SAV', 'Advantage Savings',            25000.00),
  ('CC-VISA', 'Customized Cash Rewards Visa', 15000.00);

INSERT INTO account (account_number, cif, product_code, nickname, status, ledger_balance, daily_transfer_limit, opened_on) VALUES
  ('104000123455', '100000001', 'DDA-CHK', 'Advantage Checking', 'OPEN',   4812.33, 5000.00, '2014-03-11'),
  ('104000123463', '100000001', 'DDA-SAV', 'Advantage Savings',  'OPEN',  22150.00, 5000.00, '2014-03-11'),
  ('440000528719', '100000001', 'CC-VISA', 'Customized Cash Rewards Visa', 'OPEN', -1284.56, NULL, '2017-06-19'),
  ('211000048218', '100000002', 'DDA-CHK', 'Everyday Checking',  'OPEN',   1045.12, 2500.00, '2019-08-02'),
  ('211000048226', '100000002', 'DDA-SAV', 'Rainy Day',          'OPEN',   9300.00, 2500.00, '2019-08-02'),
  ('104000198762', '100000003', 'DDA-CHK', 'Premier Checking',   'OPEN',  38420.75, 25000.00, '2008-01-24'),
  ('211000048234', '100000004', 'DDA-CHK', 'Everyday Checking',  'FROZEN',  212.08, 1000.00, '2022-11-15'),
  ('307000555010', NULL,        'GL-SUSP', 'Payments suspense',  'OPEN',      0.00, NULL,    '2001-01-01');

INSERT INTO funds_hold (account_number, amount, reason, placed_by, release_on, status) VALUES
  ('104000123455', 100.00, 'CHK-DEP-HOLD', 'teller01', '2026-11-28', 'ACTIVE');

INSERT INTO app_user (username, password_hash, role, cif, enabled) VALUES
  ('dana.whitfield',   '2dbaa2bcba911b55e49a4313817c64fb', 'CUSTOMER', '100000001', TRUE),
  ('marcus.oyelaran',  '2dbaa2bcba911b55e49a4313817c64fb', 'CUSTOMER', '100000002', TRUE),
  ('priya.raghunath',  '2dbaa2bcba911b55e49a4313817c64fb', 'CUSTOMER', '100000003', TRUE),
  ('teller01',         'b53756066001056089b855f7e2ef449b', 'TELLER',   NULL,        TRUE),
  ('ops.batch',        '47f770775b80ddf1309ecd9c0bb5120a', 'OPS',      NULL,        TRUE),
  ('svc.payments',     'eef37e9192035b6b58bc395b062b0161', 'OPS',      NULL,        TRUE);
