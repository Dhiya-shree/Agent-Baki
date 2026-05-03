# Baki Frontend - Application Management System

A user-friendly React frontend for managing application issues and tracking fixes.

## Features

- 📋 View list of all applications with issue statistics
- 🔍 Detailed view of each application with grouped issues
- ✅ Manage issue status with 4 action buttons:
  - **Ignore**: Mark issue as ignored with reason
  - **DB Fix**: Mark as database fix with change number
  - **Resolved**: Mark issue as resolved
  - **In Progress**: Mark as in progress with GitHub PR reference
- 🎨 Modern, responsive UI with color-coded status badges
- ⚡ Real-time updates after status changes

## Prerequisites

- Node.js (v14 or higher)
- npm or yarn
- Backend API running on http://localhost:8080

## Installation

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

## Running the Application

Start the development server:
```bash
npm start
```

The application will open at http://localhost:3000

## Building for Production

Create an optimized production build:
```bash
npm run build
```

The build files will be in the `build` directory.

## Project Structure

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/
│   │   ├── ApplicationList.js      # List of all applications
│   │   ├── ApplicationList.css
│   │   ├── ApplicationDetails.js   # Detailed view with issues
│   │   ├── ApplicationDetails.css
│   │   ├── FixItem.js              # Individual issue with actions
│   │   └── FixItem.css
│   ├── services/
│   │   └── api.js                  # API service layer
│   ├── App.js                      # Main application component
│   ├── App.css
│   ├── index.js                    # Entry point
│   └── index.css
├── package.json
└── README.md
```

## API Endpoints Used

- `GET /api/applications` - Get all applications
- `GET /api/applications/{id}` - Get application details with fixes
- `PUT /api/fixes/{id}/ignore` - Mark fix as ignored
- `PUT /api/fixes/{id}/in-progress` - Mark fix as in progress
- `PUT /api/fixes/{id}/db-fix` - Mark fix as DB fix
- `PUT /api/fixes/{id}/resolved` - Mark fix as resolved

## Usage

1. **View Applications**: The home page displays all applications with issue counts
2. **Select Application**: Click on any application card to view its issues
3. **Manage Issues**: Each issue has 4 action buttons:
   - Click **Ignore** to mark as ignored (requires reason)
   - Click **DB Fix** to mark as database fix (requires change number)
   - Click **Resolved** to mark as resolved (no input required)
   - Click **In Progress** to mark as in progress (requires GitHub PR)
4. **Navigate Back**: Use the back button to return to the applications list

## Troubleshooting

### CORS Issues
If you encounter CORS errors, ensure the backend has CORS configured for http://localhost:3000

### API Connection Failed
- Verify the backend is running on http://localhost:8080
- Check the API_BASE_URL in `src/services/api.js`

### Build Errors
- Delete `node_modules` and `package-lock.json`
- Run `npm install` again

## Technologies Used

- React 18
- Axios for API calls
- CSS3 with modern features
- Responsive design

## License

© 2026 Baki Application Management System