import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext.js';
import Navbar from './components/Navbar.js';
import ProtectedRoute from './components/ProtectedRoute.js';
import Login from './components/Login.js';
import AdminDashboard from './pages/admin/AdminDashboard.js';
import ManageCustomers from './pages/admin/ManageCustomers.js';
import ManageAccounts from './pages/admin/ManageAccounts.js';
import AddReading from './pages/admin/AddReading.js';
import GenerateBills from './pages/admin/GenerateBills.js';
import ManageTariffs from './pages/admin/ManageTariffs.js';
import ManageAdmins from './pages/admin/ManageAdmins.js';
import ComplaintsReport from './pages/admin/ComplaintsReport.js';
import CustomerDashboard from './pages/customer/CustomerDashboard.js';
import ViewBills from './pages/customer/ViewBills.js';
import PayBill from './pages/customer/PayBill.js';
import AdvancePayment from './pages/customer/AdvancePayment.js';
import UsageChart from './pages/customer/UsageChart.js';
import Complaints from './pages/customer/Complaints.js';
import Signup from './pages/public/Signup.js';

const AppContent = () => {
  const { user } = useAuth();

  return (
    <div className={user ? 'app-shell' : ''}>
      {user && <Navbar />}
      <main className={user ? 'app-main' : ''}>
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />

          <Route
            path="/admin/dashboard"
            element={(
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <AdminDashboard />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/admin/customers"
            element={(
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <ManageCustomers />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/admin/accounts"
            element={(
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <ManageAccounts />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/admin/readings"
            element={(
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <AddReading />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/admin/bills"
            element={(
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <GenerateBills />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/admin/tariffs"
            element={(
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <ManageTariffs />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/admin/users"
            element={(
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <ManageAdmins />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/admin/complaints"
            element={(
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <ComplaintsReport />
              </ProtectedRoute>
            )}
          />

          <Route
            path="/customer/dashboard"
            element={(
              <ProtectedRoute allowedRoles={["CUSTOMER"]}>
                <CustomerDashboard />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/customer/bills"
            element={(
              <ProtectedRoute allowedRoles={["CUSTOMER"]}>
                <ViewBills />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/customer/pay"
            element={(
              <ProtectedRoute allowedRoles={["CUSTOMER"]}>
                <PayBill />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/customer/advance-payment"
            element={(
              <ProtectedRoute allowedRoles={["CUSTOMER"]}>
                <AdvancePayment />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/customer/usage"
            element={(
              <ProtectedRoute allowedRoles={["CUSTOMER"]}>
                <UsageChart />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/customer/complaints"
            element={(
              <ProtectedRoute allowedRoles={["CUSTOMER"]}>
                <Complaints />
              </ProtectedRoute>
            )}
          />

          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </main>
    </div>
  );
};

const App = () => (
  <AuthProvider>
    <AppContent />
  </AuthProvider>
);

export default App;
