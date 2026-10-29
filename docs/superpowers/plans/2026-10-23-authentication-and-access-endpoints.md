# Inventário estático de endpoints — autenticação e acessos

Extraído por leitura dos controllers do checkout, sem executar requisições. Regras em auth/config/SecurityConfig e checagens de campo nos services. Todas as escritas exigem CSRF. Nenhum prefixo /auth/** foi liberado em bloco. HEAD de consultas privadas acompanha autorização de leitura; métodos sem handler continuam indisponíveis.

| Método | Rota backend (frontend usa /api) | Autorização | Controller |
| --- | --- | --- | --- |
| POST | `/auth/activity` | Sessão própria válida; CSRF em escritas | auth/AuthenticationController.java |
| GET | `/auth/csrf` | Público; CSRF em escritas; contenção | auth/AuthenticationController.java |
| POST | `/auth/invitationAcceptance` | Público; CSRF em escritas; contenção | auth/invitation/UserInvitationController.java |
| POST | `/auth/login` | Público; CSRF em escritas; contenção | auth/AuthenticationController.java |
| POST | `/auth/logout` | Público; CSRF em escritas; contenção | auth/AuthenticationController.java |
| PATCH | `/auth/password` | Sessão própria válida; CSRF em escritas | auth/password/PasswordRecoveryController.java |
| POST | `/auth/passwordRecovery` | Público; CSRF em escritas; contenção | auth/password/PasswordRecoveryController.java |
| POST | `/auth/passwordReset` | Público; CSRF em escritas; contenção | auth/password/PasswordRecoveryController.java |
| GET | `/auth/session` | Sessão própria válida; CSRF em escritas | auth/AuthenticationController.java |
| POST | `/auth/sessionReplacement` | Público; CSRF em escritas; contenção | auth/AuthenticationController.java |
| GET | `/auth/sessions` | Sessão própria válida; CSRF em escritas | auth/session/UserSessionController.java |
| DELETE | `/auth/sessions/{id}` | Sessão própria válida; CSRF em escritas | auth/session/UserSessionController.java |
| GET | `/brand` | products.read | product/brand/BrandController.java |
| POST | `/brand` | products.create | product/brand/BrandController.java |
| DELETE | `/brand/{id}` | products.delete | product/brand/BrandController.java |
| GET | `/brand/{id}` | products.read | product/brand/BrandController.java |
| GET | `/category` | products.read | product/category/CategoryController.java |
| POST | `/category` | products.create | product/category/CategoryController.java |
| DELETE | `/category/{id}` | products.delete | product/category/CategoryController.java |
| GET | `/category/{id}` | products.read | product/category/CategoryController.java |
| POST | `/customer` | customers.create | customer/CustomerController.java |
| GET | `/customer/search` | customers.read | customer/CustomerController.java |
| DELETE | `/customer/{id}` | customers.delete | customer/CustomerController.java |
| GET | `/customer/{id}` | customers.read | customer/CustomerController.java |
| PATCH | `/customer/{id}` | customers.update | customer/CustomerController.java |
| POST | `/customerAddress` | customers.create | customer/address/CustomerAddressController.java |
| GET | `/customerAddress/search` | customers.read | customer/address/CustomerAddressController.java |
| DELETE | `/customerAddress/{id}` | customers.delete | customer/address/CustomerAddressController.java |
| GET | `/customerAddress/{id}` | customers.read | customer/address/CustomerAddressController.java |
| PATCH | `/customerAddress/{id}` | customers.update | customer/address/CustomerAddressController.java |
| POST | `/order` | orders.create | order/OrderController.java |
| GET | `/order/search` | orders.read | order/OrderController.java |
| DELETE | `/order/{id}` | orders.delete | order/OrderController.java |
| GET | `/order/{id}` | orders.read | order/OrderController.java |
| PATCH | `/order/{id}` | orders.update; transições também exigem orders.confirm/cancel | order/OrderController.java |
| POST | `/order/{id}/dispatch` | orders.dispatch | order/dispatch/OrderDispatchController.java |
| POST | `/orderItem` | orders.create; preço/desconto explícitos também exigem orders.manualPrice/discount | order/item/OrderItemController.java |
| GET | `/orderItem/search` | orders.read | order/item/OrderItemController.java |
| DELETE | `/orderItem/{id}` | orders.delete | order/item/OrderItemController.java |
| GET | `/orderItem/{id}` | orders.read | order/item/OrderItemController.java |
| PATCH | `/orderItem/{id}` | orders.update; preço/desconto explícitos também exigem orders.manualPrice/discount | order/item/OrderItemController.java |
| POST | `/permission` | Autenticado + CSRF → 405 | user/config/permission/PermissionController.java |
| GET | `/permission/search` | access.roles.manage | user/config/permission/PermissionController.java |
| DELETE | `/permission/{id}` | Autenticado + CSRF → 405 | user/config/permission/PermissionController.java |
| GET | `/permission/{id}` | access.roles.manage | user/config/permission/PermissionController.java |
| PATCH | `/permission/{id}` | Autenticado + CSRF → 405 | user/config/permission/PermissionController.java |
| POST | `/price-table` | prices.create | price/PriceTableController.java |
| GET | `/price-table/search` | prices.read | price/PriceTableController.java |
| DELETE | `/price-table/{id}` | prices.delete | price/PriceTableController.java |
| GET | `/price-table/{id}` | prices.read | price/PriceTableController.java |
| PATCH | `/price-table/{id}` | prices.update | price/PriceTableController.java |
| POST | `/priceTableItem` | prices.create | price/item/PriceTableItemController.java |
| GET | `/priceTableItem/search` | prices.read | price/item/PriceTableItemController.java |
| DELETE | `/priceTableItem/{id}` | prices.delete | price/item/PriceTableItemController.java |
| GET | `/priceTableItem/{id}` | prices.read | price/item/PriceTableItemController.java |
| PATCH | `/priceTableItem/{id}` | prices.update | price/item/PriceTableItemController.java |
| POST | `/product` | products.create; estoque inicial também exige stock.configure | product/ProductController.java |
| GET | `/product/search` | products.read | product/ProductController.java |
| DELETE | `/product/{id}` | products.delete | product/ProductController.java |
| GET | `/product/{id}` | products.read | product/ProductController.java |
| PATCH | `/product/{id}` | products.update | product/ProductController.java |
| POST | `/productSupplier` | suppliers.update | supplier/product/ProductSupplierController.java |
| GET | `/productSupplier/search` | suppliers.read | supplier/product/ProductSupplierController.java |
| DELETE | `/productSupplier/{id}` | suppliers.update | supplier/product/ProductSupplierController.java |
| GET | `/productSupplier/{id}` | suppliers.read | supplier/product/ProductSupplierController.java |
| PATCH | `/productSupplier/{id}` | suppliers.update | supplier/product/ProductSupplierController.java |
| POST | `/purchase` | purchases.create | purchase/PurchaseController.java |
| GET | `/purchase/search` | purchases.read | purchase/PurchaseController.java |
| DELETE | `/purchase/{id}` | purchases.delete | purchase/PurchaseController.java |
| GET | `/purchase/{id}` | purchases.read | purchase/PurchaseController.java |
| PATCH | `/purchase/{id}` | purchases.update; transições também exigem purchases.send/confirm/cancel | purchase/PurchaseController.java |
| GET | `/purchase/{id}/receipts` | purchases.read | purchase/receipt/PurchaseReceiptController.java |
| POST | `/purchase/{id}/receive` | purchases.receive | purchase/receipt/PurchaseReceiptController.java |
| GET | `/purchase/{purchaseId}/receipts/{id}` | purchases.read | purchase/receipt/PurchaseReceiptController.java |
| POST | `/purchaseItem` | purchases.create | purchase/item/PurchaseItemController.java |
| GET | `/purchaseItem/search` | purchases.read | purchase/item/PurchaseItemController.java |
| DELETE | `/purchaseItem/{id}` | purchases.delete | purchase/item/PurchaseItemController.java |
| GET | `/purchaseItem/{id}` | purchases.read | purchase/item/PurchaseItemController.java |
| PATCH | `/purchaseItem/{id}` | purchases.update | purchase/item/PurchaseItemController.java |
| POST | `/role` | access.roles.manage | user/config/role/RoleController.java |
| GET | `/role/search` | access.roles.manage; GET sem /permissions também aceita access.users.manage | user/config/role/RoleController.java |
| DELETE | `/role/{id}` | access.roles.manage | user/config/role/RoleController.java |
| GET | `/role/{id}` | access.roles.manage; GET sem /permissions também aceita access.users.manage | user/config/role/RoleController.java |
| PATCH | `/role/{id}` | access.roles.manage | user/config/role/RoleController.java |
| GET | `/role/{roleId}/permissions` | access.roles.manage | user/config/RolePermissionController.java |
| POST | `/role/{roleId}/permissions` | access.roles.manage | user/config/RolePermissionController.java |
| DELETE | `/role/{roleId}/permissions/{permissionId}` | access.roles.manage | user/config/RolePermissionController.java |
| GET | `/security/audit/search` | security.audit.read | auth/audit/SecurityAuditController.java |
| GET | `/security/settings` | security.settings.manage | auth/settings/SecuritySettingsController.java |
| PATCH | `/security/settings` | security.settings.manage | auth/settings/SecuritySettingsController.java |
| POST | `/stockItem` | stock.configure | stock/StockItemController.java |
| GET | `/stockItem/search` | stock.read | stock/StockItemController.java |
| DELETE | `/stockItem/{id}` | stock.configure | stock/StockItemController.java |
| PATCH | `/stockItem/{id}` | stock.configure | stock/StockItemController.java |
| POST | `/stockMovement` | stock.adjust | stock/movement/StockMovementController.java |
| GET | `/stockMovement/search` | stock.read | stock/movement/StockMovementController.java |
| GET | `/stockMovement/{id}` | stock.read | stock/movement/StockMovementController.java |
| POST | `/supplier` | suppliers.create | supplier/SupplierController.java |
| GET | `/supplier/search` | suppliers.read | supplier/SupplierController.java |
| DELETE | `/supplier/{id}` | suppliers.delete | supplier/SupplierController.java |
| GET | `/supplier/{id}` | suppliers.read | supplier/SupplierController.java |
| PATCH | `/supplier/{id}` | suppliers.update | supplier/SupplierController.java |
| GET | `/supplier/{supplierId}/brands` | suppliers.read | supplier/brand/SupplierBrandController.java |
| POST | `/supplier/{supplierId}/brands` | suppliers.update | supplier/brand/SupplierBrandController.java |
| DELETE | `/supplier/{supplierId}/brands/{brandId}` | suppliers.update | supplier/brand/SupplierBrandController.java |
| GET | `/supplier/{supplierId}/categories` | suppliers.read | supplier/category/SupplierCategoryController.java |
| POST | `/supplier/{supplierId}/categories` | suppliers.update | supplier/category/SupplierCategoryController.java |
| DELETE | `/supplier/{supplierId}/categories/{categoryId}` | suppliers.update | supplier/category/SupplierCategoryController.java |
| POST | `/supplierAddress` | suppliers.create | supplier/address/SupplierAddressController.java |
| GET | `/supplierAddress/search` | suppliers.read | supplier/address/SupplierAddressController.java |
| DELETE | `/supplierAddress/{id}` | suppliers.delete | supplier/address/SupplierAddressController.java |
| GET | `/supplierAddress/{id}` | suppliers.read | supplier/address/SupplierAddressController.java |
| PATCH | `/supplierAddress/{id}` | suppliers.update | supplier/address/SupplierAddressController.java |
| POST | `/user` | Autenticado + CSRF → 405 | user/UserController.java |
| POST | `/user/invitation` | access.users.manage | auth/invitation/UserInvitationController.java |
| GET | `/user/invitation/search` | access.users.manage | auth/invitation/UserInvitationController.java |
| DELETE | `/user/invitation/{id}` | access.users.manage | auth/invitation/UserInvitationController.java |
| POST | `/user/invitation/{id}/resend` | access.users.manage | auth/invitation/UserInvitationController.java |
| GET | `/user/search` | access.users.manage ou security.sessions.manage | user/UserController.java |
| DELETE | `/user/{id}` | access.users.manage | user/UserController.java |
| GET | `/user/{id}` | access.users.manage ou security.sessions.manage | user/UserController.java |
| PATCH | `/user/{id}` | access.users.manage | user/UserController.java |
| POST | `/user/{id}/passwordRecovery` | access.users.manage | auth/password/PasswordRecoveryController.java |
| GET | `/user/{id}/sessions` | security.sessions.manage | auth/session/UserSessionController.java |
| DELETE | `/user/{userId}/sessions/{id}` | security.sessions.manage | auth/session/UserSessionController.java |
| POST | `/warehouse` | warehouses.create | warehouse/WarehouseController.java |
| GET | `/warehouse/search` | warehouses.read | warehouse/WarehouseController.java |
| DELETE | `/warehouse/{id}` | warehouses.delete | warehouse/WarehouseController.java |
| PATCH | `/warehouse/{id}` | warehouses.update | warehouse/WarehouseController.java |
| POST | `/warehouseAddress` | warehouses.create | warehouse/address/WarehouseAddressController.java |
| GET | `/warehouseAddress/search` | warehouses.read | warehouse/address/WarehouseAddressController.java |
| DELETE | `/warehouseAddress/{id}` | warehouses.delete | warehouse/address/WarehouseAddressController.java |
| GET | `/warehouseAddress/{id}` | warehouses.read | warehouse/address/WarehouseAddressController.java |
| PATCH | `/warehouseAddress/{id}` | warehouses.update | warehouse/address/WarehouseAddressController.java |

Custos estão incluídos nos grafos de produtos/fornecedores e associações autorizadas; não há mascaramento por cargo. Respostas de negócio continuam entidades. Campos de senha/hash/versão e credenciais de sessão ficam fora dos JSONs. As regras de autorização não substituem as regras comerciais existentes.
