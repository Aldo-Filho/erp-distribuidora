import DashboardPage from "../pages/Dashboard/DashboardPage";
import PurchaseSuggestionsPage from "../pages/Operations/PurchaseSuggestionsPage";
import ReceivablesPage from "../pages/Finance/ReceivablesPage";
import PayablesPage from "../pages/Finance/PayablesPage";
import CashFlowPage from "../pages/Finance/CashFlowPage";
import FinancialCategoriesPage from "../pages/Finance/FinancialCategoriesPage";
import BankAccountsPage from "../pages/Finance/BankAccountsPage";
import CustomerEntryPage from "../pages/Customers/CustomerEntryPage";
import CustomerDetailsPage from "../pages/Customers/CustomerDetailsPage";
import SupplierEntryPage from "../pages/Suppliers/SupplierEntryPage";
import SupplierDetailsPage from "../pages/Suppliers/SupplierDetailsPage";
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
import AppearancePage from "../pages/Settings/AppearancePage";
import CompanyPage from "../pages/Settings/CompanyPage";
import AccountPage from "../pages/Account/AccountPage";
import SuppliersPage from "../pages/Suppliers/SuppliersPage";
import CustomersPage from "../pages/Customers/CustomersPage";
import ProductEntryPage from "../pages/Products/ProductEntryPage";
import ProductDetailsPage from "../pages/Products/ProductDetailsPage";
import ProductVariantPage from "../pages/Products/ProductVariantPage";
import ProductsPage from "../pages/Products/ProductsPage";
import StockHistoryPage from "../pages/Operations/StockHistoryPage";
import StockPage from "../pages/Operations/StockPage";
import WarehousesPage from "../pages/Operations/WarehousesPage";
import OrdersPage from "../pages/Operations/OrdersPage";
import OrderEntryPage from "../pages/Operations/OrderEntryPage";
import PurchasesPage from "../pages/Operations/PurchasesPage";
import PurchaseEntryPage from "../pages/Operations/PurchaseEntryPage";
import useAuth from "../auth/useAuth";
import "../components/layout/operationPages.css";

function Home() {
  return <Navigate to="/dashboard" replace />;
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
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route element={<ProtectedRoute permission="finance.read" />}>
            <Route path="/finance/receivables" element={<ReceivablesPage />} />
            <Route path="/finance/payables" element={<PayablesPage />} />
            <Route path="/finance/cash-flow" element={<CashFlowPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="finance.configure" />}>
            <Route path="/finance/categories" element={<FinancialCategoriesPage />} />
            <Route path="/finance/bank-accounts" element={<BankAccountsPage />} />
          </Route>
          <Route path="/account" element={<AccountPage />} />
          <Route path="/security/sessions" element={<SessionsPage />} />
          <Route element={<ProtectedRoute permission="stock.read" />}>
            <Route path="/stock" element={<StockPage />} />
            <Route path="/stock/history" element={<StockHistoryPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="warehouses.read" />}>
            <Route path="/warehouses" element={<WarehousesPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="orders.read" />}>
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/orders/new" element={<OrderEntryPage />} />
            <Route path="/orders/:id" element={<OrderEntryPage />} />
            <Route path="/orders/:id/edit" element={<OrderEntryPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="purchases.read" />}>
            <Route path="/purchases" element={<PurchasesPage />} />
            <Route path="/purchase-suggestions" element={<PurchaseSuggestionsPage />} />
            <Route path="/purchases/new" element={<PurchaseEntryPage />} />
            <Route path="/purchases/:id/edit" element={<PurchaseEntryPage />} />
            <Route path="/purchases/:id" element={<PurchaseEntryPage />} />
          </Route>
          <Route element={<ProtectedRoute anyPermissions={["products.read", "stock.read"]} />}>
            <Route path="/products" element={<ProductsEntry />} />
          </Route>
          <Route element={<ProtectedRoute permission="products.read" />}>
            <Route path="/products/new" element={<ProductEntryPage />} />
            <Route path="/products/:id" element={<ProductDetailsPage />} />
            <Route path="/products/:id/edit" element={<ProductEntryPage />} />
            <Route path="/products/:id/variants/:itemId" element={<ProductVariantPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="suppliers.read" />}>
            <Route path="/suppliers" element={<SuppliersPage />} />
            <Route path="/suppliers/new" element={<SupplierEntryPage />} />
            <Route path="/suppliers/:id" element={<SupplierDetailsPage />} />
            <Route path="/suppliers/:id/edit" element={<SupplierEntryPage />} />
          </Route>
          <Route element={<ProtectedRoute permission="customers.read" />}>
            <Route path="/customers" element={<CustomersPage />} />
            <Route path="/customers/new" element={<CustomerEntryPage />} />
            <Route path="/customers/:id" element={<CustomerDetailsPage />} />
            <Route path="/customers/:id/edit" element={<CustomerEntryPage />} />
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
          <Route element={<ProtectedRoute permission="company.appearance.manage" />}>
            <Route path="/settings/appearance" element={<AppearancePage />} />
          </Route>
          <Route element={<ProtectedRoute permission="company.read" />}>
            <Route path="/settings/company" element={<CompanyPage />} />
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
