-- 1. Tạo Database
CREATE DATABASE IF NOT EXISTS SmartParkingDB
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE SmartParkingDB;

-- 2. Bảng CardHolders
CREATE TABLE CardHolders (
    HolderID INT PRIMARY KEY AUTO_INCREMENT,
    FullName VARCHAR(100) CHARACTER SET utf8mb4 NOT NULL,
    PhoneNumber VARCHAR(20) UNIQUE NOT NULL,
    IdentityCard VARCHAR(20) UNIQUE NOT NULL,
    AvatarImage LONGBLOB,
    CreatedDate DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng ParkingCards
CREATE TABLE ParkingCards (
    CardID INT PRIMARY KEY AUTO_INCREMENT,
    HolderID INT,
    CardUID VARCHAR(50) UNIQUE NOT NULL,
    LicensePlate VARCHAR(20) NOT NULL,
    VehicleType VARCHAR(20) CHARACTER SET utf8mb4,
    Balance DECIMAL(18, 0) DEFAULT 0,
    Status VARCHAR(20) DEFAULT 'ACTIVE',
    PublicKey TEXT,
    FOREIGN KEY (HolderID) REFERENCES CardHolders(HolderID)
);

-- 4. Bảng ParkingSessions
CREATE TABLE ParkingSessions (
    SessionID BIGINT PRIMARY KEY AUTO_INCREMENT,
    CardID INT,
    CheckInTime DATETIME NOT NULL,
    CheckOutTime DATETIME,
    FeeAmount DECIMAL(18, 0) DEFAULT 0,
    Status VARCHAR(20) DEFAULT 'PARKED',
    FOREIGN KEY (CardID) REFERENCES ParkingCards(CardID)
);

-- 5. Bảng TransactionLogs
CREATE TABLE TransactionLogs (
    LogID BIGINT PRIMARY KEY AUTO_INCREMENT,
    CardID INT,
    TransactionType VARCHAR(20),
    Amount DECIMAL(18, 0) NOT NULL,
    TransactionTime DATETIME DEFAULT CURRENT_TIMESTAMP,
    DigitalSignature TEXT,
    FOREIGN KEY (CardID) REFERENCES ParkingCards(CardID)
);
