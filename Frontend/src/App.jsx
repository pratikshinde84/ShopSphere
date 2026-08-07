import { useEffect, useState } from 'react';
import { GoogleLogin } from '@react-oauth/google';
import { Routes, Route, Navigate } from 'react-router-dom';
import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8082';
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

const api = axios.create({ baseURL: API_URL });

function App() {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [user, setUser] = useState(JSON.parse(localStorage.getItem('user') || 'null'));
  const [error, setError] = useState('');
  const [mockUsername, setMockUsername] = useState('selleruser');

  useEffect(() => {
    if (token) {
      api.defaults.headers.common.Authorization = `Bearer ${token}`;
    }
  }, [token]);

  const handleGoogleSuccess = async (credentialResponse) => {
    try {
      const { data } = await api.post('/api/auth/google', {
        idToken: credentialResponse.credential,
        role: 'SELLER',
        username: 'selleruser',
      });

      localStorage.setItem('token', data.data.token);
      localStorage.setItem('user', JSON.stringify(data.data.user));
      setToken(data.data.token);
      setUser(data.data.user);
      setError('');
    } catch (err) {
      setError(err?.response?.data?.message || 'Login failed');
    }
  };

  const handleMockLogin = async () => {
    try {
      const mockToken = `mock-token-${mockUsername.replace(/\s+/g, '').toLowerCase()}`;
      const { data } = await api.post('/api/auth/google', {
        idToken: mockToken,
        role: 'SELLER',
        username: mockUsername || 'selleruser',
      });

      localStorage.setItem('token', data.data.token);
      localStorage.setItem('user', JSON.stringify(data.data.user));
      setToken(data.data.token);
      setUser(data.data.user);
      setError('');
    } catch (err) {
      setError(err?.response?.data?.message || 'Mock login failed');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  return (
    <div className="container">
      <div className="topbar">
        <div>
          <h1>ShopHub Seller Portal</h1>
          <p>Manage products from a React frontend.</p>
        </div>
        {user ? (
          <button className="secondary" onClick={handleLogout}>Logout</button>
        ) : null}
      </div>

      {error ? <div className="error">{error}</div> : null}

      {!token ? (
        <div className="card">
          <h2>Sign in with Google</h2>
          {GOOGLE_CLIENT_ID ? (
            <GoogleLogin
              onSuccess={handleGoogleSuccess}
              onError={() => setError('Google login failed')}
              useOneTap={false}
            />
          ) : (
            <div className="form-grid">
              <p>Google client ID is not configured, so the app is using a local mock login flow.</p>
              <input
                placeholder="Username"
                value={mockUsername}
                onChange={(e) => setMockUsername(e.target.value)}
              />
              <button onClick={handleMockLogin}>Continue with mock seller login</button>
            </div>
          )}
        </div>
      ) : (
        <Routes>
          <Route path="/" element={<Dashboard user={user} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      )}
    </div>
  );
}

function Dashboard({ user }) {
  const [products, setProducts] = useState([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [quantity, setQuantity] = useState('');
  const [category, setCategory] = useState('');
  const [image, setImage] = useState(null);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const loadProducts = async () => {
    try {
      const { data } = await api.get('/api/seller/products');
      setProducts(data.data || []);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage('');

    const formData = new FormData();
    const productPayload = JSON.stringify({
      name,
      description,
      price: Number(price),
      quantity: Number(quantity),
      category,
    });
    formData.append('product', new Blob([productPayload], { type: 'application/json' }), 'product.json');
    if (image) formData.append('image', image);

    try {
      await api.post('/api/seller/products', formData);
      setMessage('Product created successfully');
      setName('');
      setDescription('');
      setPrice('');
      setQuantity('');
      setCategory('');
      setImage(null);
      loadProducts();
    } catch (err) {
      setMessage(err?.response?.data?.message || 'Product upload failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="grid">
      <div className="card">
        <h2>Welcome, {user?.name || user?.username || 'seller'}</h2>
        <p>Use the form below to create products for your storefront.</p>
        <form onSubmit={handleSubmit} className="form-grid">
          <input placeholder="Product name" value={name} onChange={(e) => setName(e.target.value)} required />
          <textarea placeholder="Description" value={description} onChange={(e) => setDescription(e.target.value)} required />
          <input type="number" placeholder="Price" value={price} onChange={(e) => setPrice(e.target.value)} required />
          <input type="number" placeholder="Quantity" value={quantity} onChange={(e) => setQuantity(e.target.value)} required />
          <input placeholder="Category" value={category} onChange={(e) => setCategory(e.target.value)} required />
          <input type="file" accept="image/*" onChange={(e) => setImage(e.target.files[0])} />
          <button type="submit" disabled={loading}>{loading ? 'Creating...' : 'Create Product'}</button>
        </form>
        {message ? <div className={message.includes('successfully') ? 'success' : 'error'}>{message}</div> : null}
      </div>

      <div className="card">
        <h2>Your Products</h2>
        {products.length === 0 ? <p>No products yet.</p> : products.map((product) => (
          <div key={product.id} className="product-card" style={{ marginBottom: '0.75rem' }}>
            <strong>{product.name}</strong>
            <p>{product.description}</p>
            <p>Price: ₹{product.price}</p>
            <p>Category: {product.category}</p>
            {product.imageUrl ? <img src={product.imageUrl} alt={product.name} /> : null}
          </div>
        ))}
      </div>
    </div>
  );
}

export default App;
