'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

interface OrderItem {
  id: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

interface Order {
  id: string;
  idempotencyKey: string;
  buyerId: string;
  supplierId: string;
  tenantId: string;
  status: string;
  totalAmount: number;
  currency: string;
  items: OrderItem[];
  createdAt: string;
}

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  VALIDATED: 'bg-blue-100 text-blue-700',
  PROCESSING: 'bg-yellow-100 text-yellow-700',
  SETTLED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-700',
};

export default function DashboardPage() {
  const router = useRouter();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');

  async function fetchOrders() {
    setLoading(true);
    const res = await fetch('/api/orders');
    if (res.status === 401) {
      router.push('/login');
      return;
    }
    const data = await res.json();
    setOrders(Array.isArray(data) ? data : []);
    setLoading(false);
  }

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleLogout() {
    await fetch('/api/auth/logout', { method: 'POST' });
    router.push('/login');
  }

  async function handleCreateOrder() {
    setCreating(true);
    setError('');

    const res = await fetch('/api/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        buyerId: '11111111-1111-1111-1111-111111111111',
        supplierId: '22222222-2222-2222-2222-222222222222',
        idempotencyKey: `order-bff-${Date.now()}`,
        items: [
          {
            productName: 'Notebook',
            quantity: 1,
            unitPrice: 1000.00,
            currency: 'BRL',
          },
        ],
      }),
    });

    if (res.ok) {
      await fetchOrders();
    } else {
      const data = await res.json();
      setError(JSON.stringify(data));
    }

    setCreating(false);
  }

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <div className="max-w-5xl mx-auto">

        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-800">TradeFlow</h1>
            <p className="text-gray-500 text-sm mt-1">Dashboard de Ordens</p>
          </div>
          <button
            onClick={handleLogout}
            className="bg-red-500 text-white px-4 py-2 rounded-md hover:bg-red-600 transition text-sm"
          >
            Logout
          </button>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 mb-6 flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-gray-700">Ordens</h2>
            <p className="text-gray-400 text-sm">{orders.length} ordem(ns) encontrada(s)</p>
          </div>
          <div className="flex gap-3">
            <button
              onClick={fetchOrders}
              disabled={loading}
              className="border border-gray-300 text-gray-600 px-4 py-2 rounded-md hover:bg-gray-50 disabled:opacity-50 transition text-sm"
            >
              {loading ? 'Atualizando...' : 'Atualizar'}
            </button>
            <button
              onClick={handleCreateOrder}
              disabled={creating}
              className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 disabled:opacity-50 transition text-sm"
            >
              {creating ? 'Criando...' : '+ Nova Ordem'}
            </button>
          </div>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
            <p className="text-red-600 text-sm">{error}</p>
          </div>
        )}

        <div className="bg-white rounded-lg shadow-sm overflow-hidden">
          {loading && orders.length === 0 ? (
            <div className="p-12 text-center text-gray-400">Carregando ordens...</div>
          ) : orders.length === 0 ? (
            <div className="p-12 text-center text-gray-400">Nenhuma ordem encontrada.</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-6 py-3 text-gray-500 font-medium">ID</th>
                  <th className="text-left px-6 py-3 text-gray-500 font-medium">Status</th>
                  <th className="text-left px-6 py-3 text-gray-500 font-medium">Total</th>
                  <th className="text-left px-6 py-3 text-gray-500 font-medium">Itens</th>
                  <th className="text-left px-6 py-3 text-gray-500 font-medium">Criado em</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {orders.map((order) => (
                  <tr key={order.id} className="hover:bg-gray-50 transition">
                    <td className="px-6 py-4 font-mono text-xs text-gray-500">
                      {order.id.substring(0, 8)}...
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${STATUS_COLORS[order.status] ?? 'bg-gray-100 text-gray-600'}`}>
                        {order.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-gray-700 font-medium">
                      {order.currency} {Number(order.totalAmount).toFixed(2)}
                    </td>
                    <td className="px-6 py-4 text-gray-500">
                      {order.items.length} item(s)
                    </td>
                    <td className="px-6 py-4 text-gray-400">
                      {new Date(order.createdAt).toLocaleString('pt-BR')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

      </div>
    </div>
  );
}