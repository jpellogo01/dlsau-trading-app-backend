# GAWT Backend - New Features Documentation

## Overview

This document outlines the new features added to the GAWT backend system, including user profile fields, OTP verification via Brevo, and Cloudinary integration for profile pictures.

## New Features

### 1. User Profile Fields

The User model now includes additional fields:

- `firstName` - User's first name
- `lastName` - User's last name
- `program` - User's program/course
- `profilePicture` - URL of user's profile picture (stored in Cloudinary)

### 2. OTP Email Verification

Users must verify their email via OTP during registration:

- OTP is sent via Brevo SMTP immediately after registration
- OTP expires after 10 minutes (configurable in `application.properties`)
- Users can resend OTP if needed
- Email verification is required before logging in

### 3. Cloudinary Integration

Profile pictures are uploaded to Cloudinary:

- Images are stored securely in the cloud
- Automatic image optimization
- Easy sharing and CDN delivery
- Old images are automatically deleted when updating

## API Endpoints

### Authentication Endpoints

#### 1. Register User

```
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "password": "secure_password",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "program": "Computer Science",
  "role": "USER"
}

Response:
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "program": "Computer Science",
  "role": "USER",
  "isVerified": false,
  "message": "User registered successfully. Please verify your email with the OTP sent to john@example.com"
}
```

#### 2. Verify OTP

```
POST /api/auth/verify-otp
Content-Type: application/json

{
  "email": "john@example.com",
  "otp": "123456"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "program": "Computer Science",
  "profilePicture": null,
  "role": "USER",
  "isVerified": true,
  "message": "Email verified successfully"
}
```

#### 3. Resend OTP

```
POST /api/auth/resend-otp?email=john@example.com

Response:
{
  "email": "john@example.com",
  "isVerified": false,
  "message": "OTP resent successfully to john@example.com"
}
```

#### 4. Login

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "secure_password"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "program": "Computer Science",
  "profilePicture": "https://cloudinary.com/...",
  "role": "USER",
  "isVerified": true,
  "message": "Login successful"
}
```

### User Profile Endpoints

#### 1. Get Profile

```
GET /api/user/profile
Authorization: Bearer <token>

Response:
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "program": "Computer Science",
  "profilePicture": "https://cloudinary.com/...",
  "role": "USER",
  "isVerified": true
}
```

#### 2. Upload Profile Picture

```
POST /api/user/upload-profile-picture
Content-Type: multipart/form-data
Authorization: Bearer <token>

Form Data:
- file: <image file>

Response:
{
  "message": "Profile picture uploaded successfully",
  "profilePictureUrl": "https://cloudinary.com/gawt/profile_pictures/user_1.jpg"
}
```

#### 3. Update Profile

```
PUT /api/user/update-profile
Content-Type: application/json
Authorization: Bearer <token>

{
  "firstName": "John",
  "lastName": "Doe",
  "program": "Information Technology"
}

Response:
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "program": "Information Technology",
  "profilePicture": "https://cloudinary.com/...",
  "role": "USER",
  "isVerified": true,
  "message": "Profile updated successfully"
}
```

## Environment Variables

Create a `.env` file or set these environment variables:

```
# Brevo Email Configuration
BREVO_SMTP_PASSWORD=<your-brevo-smtp-password>
BREVO_API_KEY=<your-brevo-api-key>

# Cloudinary Configuration
CLOUDINARY_CLOUD_NAME=<your-cloud-name>
CLOUDINARY_API_KEY=<your-api-key>
CLOUDINARY_API_SECRET=<your-api-secret>

# Database
MYSQL_URL=jdbc:mysql://localhost:3306/gawt_db
MYSQL_USERNAME=root
MYSQL_PASSWORD=password
```

## Configuration

Update `application.properties`:

```properties
# OTP Configuration
otp.expiry.minutes=10
otp.length=6

# Email Configuration (Brevo)
spring.mail.host=smtp-relay.brevo.com
spring.mail.port=587
spring.mail.username=no-reply@dlsauwebtube.com
spring.mail.password=${BREVO_SMTP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
brevo.api.key=${BREVO_API_KEY}
brevo.sender=no-reply@dlsauwebtube.com

# Cloudinary Configuration
cloudinary.cloud.name=${CLOUDINARY_CLOUD_NAME}
cloudinary.api.key=${CLOUDINARY_API_KEY}
cloudinary.api.secret=${CLOUDINARY_API_SECRET}
```

## Services

### EmailService

Handles all email operations:

- `generateOTP()` - Generates a random OTP
- `sendOTPEmail()` - Sends OTP email via Brevo
- `sendVerificationSuccessEmail()` - Sends verification success email

### CloudinaryService

Handles profile picture management:

- `uploadProfilePicture()` - Upload image to Cloudinary
- `deleteProfilePicture()` - Delete image from Cloudinary
- `updateProfilePicture()` - Replace existing image

## Database Changes

New columns added to `users` table:

- `first_name` (VARCHAR)
- `last_name` (VARCHAR)
- `program` (VARCHAR)
- `profile_picture` (TEXT - URL)
- `otp_code` (VARCHAR)
- `otp_expiry_time` (BIGINT)
- `is_verified` (BOOLEAN, default: false)

Run migrations:

```sql
ALTER TABLE users ADD COLUMN first_name VARCHAR(255);
ALTER TABLE users ADD COLUMN last_name VARCHAR(255);
ALTER TABLE users ADD COLUMN program VARCHAR(255);
ALTER TABLE users ADD COLUMN profile_picture TEXT;
ALTER TABLE users ADD COLUMN otp_code VARCHAR(10);
ALTER TABLE users ADD COLUMN otp_expiry_time BIGINT;
ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT false;
```

## Dependencies Added

```xml
<!-- Spring Mail -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- Cloudinary -->
<dependency>
    <groupId>com.cloudinary</groupId>
    <artifactId>cloudinary-http44</artifactId>
    <version>1.36.0</version>
</dependency>

<!-- HttpClient -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.2.1</version>
</dependency>

<!-- GSON -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

## Setup Instructions

1. **Install Dependencies**

   ```bash
   mvn clean install
   ```

2. **Configure Environment Variables**
   - Set up Brevo account and get SMTP credentials
   - Set up Cloudinary account and get API credentials
   - Add to `.env` or system environment variables

3. **Update Database Configuration**
   - Update MySQL connection details in `application.properties`

4. **Run Migrations**
   - Execute the SQL migration commands above

5. **Start Application**
   ```bash
   mvn spring-boot:run
   ```

## Security Notes

- OTP is sent via secure email (Brevo SMTP)
- Passwords are encrypted using BCrypt
- JWT tokens are used for authentication
- Profile endpoint requires authentication
- All sensitive data is stored securely

## Error Handling

Common error responses:

```json
// Invalid OTP
{
  "error": "Invalid OTP"
}

// OTP Expired
{
  "error": "OTP has expired. Please register again"
}

// Email already in use
{
  "error": "Email is already in use"
}

// User not verified
{
  "error": "Please verify your email before logging in"
}
```

## Testing

### Register and Verify User

1. Call `/api/auth/register` with new user details
2. Check email for OTP
3. Call `/api/auth/verify-otp` with OTP
4. Use returned token for login

### Upload Profile Picture

1. Get authentication token from login
2. Call `/api/user/upload-profile-picture` with image file
3. Verify image URL in profile

## Support

For issues or questions:

- Check application logs for error messages
- Verify Brevo and Cloudinary API keys are correct
- Ensure MySQL is running and accessible
- Check JWT configuration

## Future Enhancements

- Two-factor authentication
- Social login integration
- Picture cropping and filtering
- Profile verification badges
- Email notification preferences
