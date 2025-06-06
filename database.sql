-- Create database
CREATE DATABASE IF NOT EXISTS cinema_db;
USE cinema_db;

-- Create roles table
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Create users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    role_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Create movies table
CREATE TABLE movies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration INT NOT NULL,
    genre VARCHAR(100),
    release_date DATE,
    image_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create screenings table
CREATE TABLE screenings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    hall_number INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    total_seats INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

-- Create seats table
CREATE TABLE seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hall_number INT NOT NULL,
    row_num INT NOT NULL,
    seat_number INT NOT NULL,
    seat_name VARCHAR(10)
);

-- Create seat_reservations table
CREATE TABLE seat_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    screening_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    is_reserved BOOLEAN DEFAULT FALSE,
    is_confirmed BOOLEAN DEFAULT FALSE,
    reservation_expiry DATETIME,
    lock_version INT DEFAULT NULL,
    FOREIGN KEY (screening_id) REFERENCES screenings(id),
    FOREIGN KEY (seat_id) REFERENCES seats(id)
);

-- Create tickets table
CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    screening_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    purchase_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status enum('ACTIVE','CANCELED','USED') NOT NULL,
    version BIGINT DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (screening_id) REFERENCES screenings(id),
    FOREIGN KEY (seat_id) REFERENCES seats(id)
);


-- Insert default roles
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_CUSTOMER');

-- Insert default admin user (password: admin)
INSERT IGNORE INTO users (username, email, password, role_id) 
VALUES ('admin', 'admin@example.com', '$2a$12$Ab.jMaB.exv4hlf1ayaJUevL33oWAFR0OOt52Xt27gBAGS/g7t3Hm', 1)
