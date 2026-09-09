# GAWT Backend Setup & Implementation Guide

## Quick Start - What Was Changed

### 1. **User Model Updates** ✅

Added 7 new fields to User entity:

- `firstName` (VARCHAR)
- `lastName` (VARCHAR)
- `program` (VARCHAR)
- `profilePicture` (TEXT - URL)
- `otpCode` (VARCHAR)
- `otpExpiryTime` (BIGINT - milliseconds)
- `isVerified` (BOOLEAN - default: false)

### 2. **DTOs Updated** ✅

- **RegisterRequest**: Added firstName, lastName, program fields
- **AuthResponse**: Added all new profile fields + isVerified flag
- **New VerifyOTPRequest**: For OTP verification endpoint
- **New UpdateProfileRequest**: For profile updates

### 3. **Email Service Created** ✅

New `EmailService.java` handles:

- OTP generation (random 6-digit codes)
- OTP email sending via Brevo SMTP
- Verification success emails

### 4. **Cloudinary Service Created** ✅

New `CloudinaryService.java` handles:

- Profile picture uploads to Cloudinary
- Automatic image deletion when updating
- Secure URL storage in database

### 5. **Auth Service Enhanced** ✅

Modified `AuthService.java`:

- Registration now creates unverified users + sends OTP
- Requires email verification before login
- New `verifyOTP()` method for OTP validation
- New `resendOTP()` method for resending codes

### 6. **Auth Controller Updated** ✅

Added new endpoints:

- `POST /api/auth/verify-otp` - Verify email with OTP
- `POST /api/auth/resend-otp` - Resend OTP to email

### 7. **New User Profile Controller** ✅

New `UserProfileController.java`:

- `POST /api/user/upload-profile-picture` - Upload picture
- `PUT /api/user/update-profile` - Update profile info
- `GET /api/user/profile` - Get user profile

### 8. **Dependencies Added** ✅

- Spring Mail (Brevo SMTP)
- Cloudinary SDK
- HttpClient 5
- GSON

### 9. **Configuration Files** ✅

- Updated `application.properties` with email & Cloudinary config
- Created `.env.example` template

## Setup Instructions

### Step 1: Get API Keys

#### Brevo Email

1. Go to https://www.brevo.com
2. Sign up / Log in
3. Get SMTP credentials:
   - Host: smtp-relay.brevo.com
   - Port: 587
   - Username: no-reply@dlsauwebtube.com (given)
   - Password: `${BREVO_SMTP_PASSWORD}`
4. Get API Key: Settings → API Keys

#### Cloudinary

1. Go to https://cloudinary.com
2. Sign up / Log in
3. Go to Dashboard → Settings
4. Get credentials:
   - Cloud Name
   - API Key
   - API Secret

### Step 2: Set Environment Variables

**Option A: System Environment Variables**

```powershell
# On Windows PowerShell (run as admin)
[Environment]::SetEnvironmentVariable("BREVO_SMTP_PASSWORD", "your_password", "User")
[Environment]::SetEnvironmentVariable("BREVO_API_KEY", "your_api_key", "User")
[Environment]::SetEnvironmentVariable("CLOUDINARY_CLOUD_NAME", "your_cloud_name", "User")
[Environment]::SetEnvironmentVariable("CLOUDINARY_API_KEY", "your_api_key", "User")
[Environment]::SetEnvironmentVariable("CLOUDINARY_API_SECRET", "your_api_secret", "User")

# Then restart IDE/Terminal
```

**Option B: IDE Configuration**

1. Right-click project → Properties/Settings
2. Add environment variables
3. Or create `.env` file and use IDE's env plugin

### Step 3: Update Database

Run these migration SQL commands:

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS program VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_picture TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_code VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_expiry_time BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT false;

CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_username ON users(username);
```

Or let Hibernate handle it with:

```properties
spring.jpa.hibernate.ddl-auto=update
```

### Step 4: Build & Run

```bash
# Clean build
mvn clean install

# Run application
mvn spring-boot:run
```

## Testing the Implementation

### 1. Register New User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123",
    "email": "test@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "program": "Computer Science",
    "role": "USER"
  }'
```

Check email for OTP (check spam folder too)

### 2. Verify OTP

```bash
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "otp": "123456"
  }'
```

Save the returned JWT token

### 3. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123"
  }'
```

### 4. Get Profile

```bash
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer <your_jwt_token>"
```

### 5. Upload Profile Picture

```bash
curl -X POST http://localhost:8080/api/user/upload-profile-picture \
  -H "Authorization: Bearer <your_jwt_token>" \
  -F "file=@/path/to/image.jpg"
```

### 6. Update Profile

```bash
curl -X PUT http://localhost:8080/api/user/update-profile \
  -H "Authorization: Bearer <your_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "program": "Information Technology"
  }'
```

## Frontend Integration

### Registration Flow

1. Show form with: username, password, email, firstName, lastName, program
2. Call `/api/auth/register`
3. Show "Verify OTP" screen
4. Call `/api/auth/verify-otp` after user enters OTP
5. Store token in localStorage

### Login Flow

1. Show username/password form
2. Call `/api/auth/login`
3. Check email verification: if `isVerified: false`, redirect to OTP verification
4. Store token on success

### Profile Management

1. Call `/api/user/profile` to fetch current profile
2. Show profile form with auto-filled data
3. Allow image upload via multipart form
4. Allow profile field updates via PUT

## Troubleshooting

### Email Not Sending

- Check Brevo SMTP credentials
- Verify email configuration in application.properties
- Check application logs for SMTP errors
- Test with Brevo's email dashboard

### Cloudinary Upload Not Working

- Verify Cloud Name, API Key, API Secret
- Check image file size (limit: 10MB)
- Enable unsigned uploads in Cloudinary settings if needed
- Check folder permissions

### OTP Issues

- Verify OTP expiry time in application.properties
- Check clock sync on server
- Clear browser cache if testing same email

### Database Migration Issues

- Ensure MySQL is running
- Check connection credentials
- Run migrations manually if auto-ddl fails
- Check database user permissions

## Security Checklist

- [ ] Passwords encrypted with BCrypt
- [ ] JWT tokens created for verified users
- [ ] OTP sent securely via email
- [ ] Profile pictures served via Cloudinary CDN
- [ ] API endpoints require authentication
- [ ] Environment variables stored securely (not in code)
- [ ] HTTPS enabled in production
- [ ] CORS properly configured for frontend URL

## Performance Tips

1. **Image Optimization**: Cloudinary auto-optimizes images
2. **Caching**: Add caching headers for profile pictures
3. **Database**: Add indexes on frequently queried fields
4. **Email**: Consider async OTP sending for scale
5. **JWT**: Verify tokens efficiently in filter

## Next Steps

1. ✅ Complete setup above
2. ✅ Test all endpoints manually
3. ✅ Integrate with frontend
4. ✅ Add rate limiting to prevent OTP spam
5. ✅ Add email confirmation for new addresses
6. ✅ Implement two-factor authentication
7. ✅ Add user profile verification badges

## Common Issues & Solutions

| Issue                  | Solution                                      |
| ---------------------- | --------------------------------------------- |
| 401 Unauthorized       | Token expired or invalid - re-login           |
| 403 Forbidden          | User not verified - complete OTP verification |
| 500 Email Error        | Check Brevo credentials and network           |
| 500 Cloudinary Error   | Verify API credentials and image format       |
| OTP not received       | Check spam folder, verify Brevo account       |
| Password hash mismatch | Ensure PasswordEncoder is properly configured |

## File Checklist

✅ User.java - Model updated
✅ RegisterRequest.java - DTO updated
✅ AuthResponse.java - DTO updated
✅ VerifyOTPRequest.java - NEW
✅ UpdateProfileRequest.java - NEW
✅ EmailService.java - NEW
✅ CloudinaryService.java - NEW
✅ AuthService.java - Enhanced
✅ AuthController.java - Updated
✅ UserProfileController.java - NEW
✅ application.properties - Updated
✅ pom.xml - Dependencies added
✅ .env.example - NEW

## Support & Documentation

Refer to:

- [NEW_FEATURES.md](NEW_FEATURES.md) - Complete API documentation
- [Brevo Docs](https://developers.brevo.com/)
- [Cloudinary Docs](https://cloudinary.com/documentation)
- [Spring Mail Docs](https://spring.io/guides/gs/sending-email/)
