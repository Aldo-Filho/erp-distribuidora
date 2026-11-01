import { Navigate, Route, Routes } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";
import AppLayout from "../components/layout/AppLayout";
import LoginPage from "../pages/Auth/LoginPage";
import PasswordRecoveryPage from "../pages/Auth/PasswordRecoveryPage";
import InvitationAcceptancePage from "../pages/Auth/InvitationAcceptancePage";
import PasswordResetPage from "../pages/Auth/PasswordResetPage";
import UsersPage from "../pages/Access/UsersPage";
import RolesPage from "../pages/Access/RolesPage";
import InvitationsPage from "../pages/Access/InvitationsPage";
import SessionsPage from "../pages/Security/SessionsPage";
import SecuritySettingsPage from "../pages/Security/SecuritySettingsPage";
import SecurityAuditPage from "../pages/Security/SecurityAuditPage";
import AccountPage from "../pages/Account/AccountPage";
import SuppliersPage from "../pages/Suppliers/SuppliersPage";
import CustomersPage from "../pages/Customers/CustomersPage";
import ProductsPage from "../pages/Products/ProductsPage";
import StockPage from "../pages/Operations/StockPage";
import WarehousesPage from "../pages/Operations/WarehousesPage";
import OrdersPage from "../pages/Operations/OrdersPage";
import OrderEntryPage from "../pages/Operations/OrderEntryPage";
import PurchasesPage from "../pages/Operations/PurchasesPage";
import useAuth from "../auth/useAuth";
import { firstRoute } from "./navigation";

function Home() {
  const { session } = useAuth();
  return <Navigate to={firstRoute(session)} replace />;
}

function ProductsEntry() {
  const { has } = useAuth();
  return has("products.read") ? <ProductsPage /> : <StockPage />;
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/forgot-password" element={<PasswordRecoveryPage />} />
      <Route path="/accept-invitation" element={<InvitationAcceptancePage />} />
      <Route path="/reset-password" element={<PasswordResetPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<Home />} />
          <Route path="/account" element={<AccountPage />} />
          <Route path="/security/sessions" element={<SessionsPage />} />
          <Route element={<ProtectedRoute permission="stock.read" />}>
            <Route path="/stock" element={<Navigate to="/products" replace />} />
          </Route>
          <Route element={<ProtectedRoute permission="warehouses.read" />}>
            <Route path="/warehouses" element={<WarehousesPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="orders.read" />}>
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/orders/new" element={<OrderEntryPage />} />
            <Route path="/orders/:id/edit" element={<OrderEntryPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="purchases.read" />}>
            <Route path="/purchases" element={<PurchasesPage />} />
          </Route>
          <Route element={<ProtectedRoute anyPermissions={["products.read", "stock.read"]} />}>
            <Route path="/products" element={<ProductsEntry />} />
          </Route>
          <Route element={<ProtectedRoute permission="suppliers.read" />}>
            <Route path="/suppliers" element={<SuppliersPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="customers.read" />}>
            <Route path="/customers" element={<CustomersPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="access.users.manage" />}>
            <Route path="/access/users" element={<UsersPage />} />
            <Route path="/access/invitations" element={<InvitationsPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="access.roles.manage" />}>
            <Route path="/access/roles" element={<RolesPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="security.settings.manage" />}>
            <Route path="/security/settings" element={<SecuritySettingsPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="security.audit.read" />}>
            <Route path="/security/audit" element={<SecurityAuditPage />} />
          </Route>
          <Route
            path="*"
            element={
              <section className="panel">
                <h1>Página não encontrada</h1>
                <a href="/account">Abrir minha conta</a>
              </section>
            }
          />
        </Route>
      </Route>
    </Routes>
  );
}
