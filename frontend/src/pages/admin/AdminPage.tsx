import { useState } from 'react'
import { AdminCategoriesPanel } from './AdminCategoriesPanel'
import { AdminOrderManagementPage } from './AdminOrderManagementPage'
import { AdminProductsPanel } from './AdminProductsPanel'

type AdminSection = 'products' | 'categories' | 'orders'

const sections: { value: AdminSection; label: string; description: string }[] = [
  { value: 'products', label: 'Products', description: 'Manage the catalog' },
  { value: 'categories', label: 'Categories', description: 'Organize your shelves' },
  { value: 'orders', label: 'Orders', description: 'Keep things moving' },
]

export function AdminPage() {
  const [activeSection, setActiveSection] = useState<AdminSection>('products')
  return (
    <div className="page-stack">
      <section className="admin-hero">
        <div>
          <span className="eyebrow eyebrow--light">StoreStack / Studio</span>
          <h1>A good shop runs<br /><em>on good details.</em></h1>
          <p>Keep your catalog tidy and your customer orders moving.</p>
        </div>
        <span className="admin-hero-seal" aria-hidden="true">SS<br />ADMIN</span>
      </section>
      <nav className="admin-tabs" aria-label="Admin sections">
        {sections.map((section) => (
          <button
            type="button"
            className={activeSection === section.value ? 'admin-tab admin-tab--active' : 'admin-tab'}
            key={section.value}
            onClick={() => setActiveSection(section.value)}
          >
            <strong>{section.label}</strong>
            <span>{section.description}</span>
          </button>
        ))}
      </nav>
      {activeSection === 'products' ? <AdminProductsPanel /> : null}
      {activeSection === 'categories' ? <AdminCategoriesPanel /> : null}
      {activeSection === 'orders' ? <AdminOrderManagementPage /> : null}
    </div>
  )
}
