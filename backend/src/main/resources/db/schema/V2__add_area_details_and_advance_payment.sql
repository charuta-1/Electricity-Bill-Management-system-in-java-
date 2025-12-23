CREATE TABLE area_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    area_name VARCHAR(255) NOT NULL,
    transformer_no VARCHAR(50) NOT NULL,
    feeder_no VARCHAR(50) NOT NULL,
    pole_no VARCHAR(50) NOT NULL,
    UNIQUE (area_name)
);

-- Insert predefined areas with random transformer, feeder, and pole numbers
INSERT INTO area_details (area_name, transformer_no, feeder_no, pole_no) VALUES
('Balaji Nagar', 'TR-101', 'FD-201', 'PL-301'),
('Katraj', 'TR-102', 'FD-202', 'PL-302'),
('Sinhagad Road', 'TR-103', 'FD-203', 'PL-303'),
('Vadgaon Budruk', 'TR-104', 'FD-204', 'PL-304'),
('Dhayari', 'TR-105', 'FD-205', 'PL-305'),
('Narhe Gaon', 'TR-106', 'FD-206', 'PL-306'),
('Ambegaon', 'TR-107', 'FD-207', 'PL-307'),
('Dhankawadi', 'TR-108', 'FD-208', 'PL-308'),
('Bibwewadi', 'TR-109', 'FD-209', 'PL-309'),
('Wakad', 'TR-110', 'FD-210', 'PL-310'),
('Hinjewadi', 'TR-111', 'FD-211', 'PL-311'),
('Thergaon', 'TR-112', 'FD-212', 'PL-312'),
('Rahatani', 'TR-113', 'FD-213', 'PL-313'),
('Pimple Nilakh', 'TR-114', 'FD-214', 'PL-314'),
('Pimple Gurav', 'TR-115', 'FD-215', 'PL-315'),
('Pimple Saudagar', 'TR-116', 'FD-216', 'PL-316'),
('Shivaji Nagar', 'TR-117', 'FD-217', 'PL-317'),
('Deccan Gymkhana', 'TR-118', 'FD-218', 'PL-318'),
('Model Colony', 'TR-119', 'FD-219', 'PL-319'),
('Shaniwar Wada', 'TR-120', 'FD-220', 'PL-320'),
('Swargate', 'TR-121', 'FD-221', 'PL-321'),
('Tilak Road', 'TR-122', 'FD-222', 'PL-322'),
('Bhandarkar Road', 'TR-123', 'FD-223', 'PL-323'),
('Ganeshkhind Road', 'TR-124', 'FD-224', 'PL-324'),
('Gokhale Nagar', 'TR-125', 'FD-225', 'PL-325'),
('Senapati Bapat Road', 'TR-126', 'FD-226', 'PL-326'),
('Prabhat Road', 'TR-127', 'FD-227', 'PL-327');

-- Add area_id to customers table (note: plural form)
ALTER TABLE customers ADD COLUMN area_id BIGINT;
ALTER TABLE customers ADD FOREIGN KEY (area_id) REFERENCES area_details(id);

-- Add advance_payment column to customers table
ALTER TABLE customers ADD COLUMN advance_payment DECIMAL(10,2) DEFAULT 0.00;