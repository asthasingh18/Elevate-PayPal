/**
 * PayBridge Centralized API Client
 *
 * Communicates with the Spring Boot backend on port 8081 (or VITE_API_URL).
 * Strict adherence to docs/API_CONTRACT.md and backend DTO specifications.
 */

// In development with the Vite proxy configured in vite.config.js,
// API_BASE defaults to empty string so requests to '/api/...' go through the Vite dev server proxy.
// If VITE_API_URL is explicitly defined (e.g. in production or cross-origin dev), it will use that base URL.
const API_BASE = import.meta.env.VITE_API_URL !== undefined ? import.meta.env.VITE_API_URL : '';

/**
 * Custom API Error wrapping backend error responses
 */
export class ApiError extends Error {
  constructor(status, message, details = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.error = details.error || 'API_ERROR';
    this.path = details.path;
    this.timestamp = details.timestamp;
    this.raw = details;
  }
}

/**
 * Standard fetch wrapper with JSON serialization and robust error normalization
 */
async function request(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const headers = {
    Accept: 'application/json',
    ...options.headers,
  };

  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  let response;
  try {
    response = await fetch(url, {
      ...options,
      headers,
    });
  } catch {
    throw new ApiError(
      0,
      `Network connection failed. Unable to reach backend at ${API_BASE || 'proxy (/api)'}. Ensure the Spring Boot service is running.`,
      { error: 'NETWORK_ERROR' }
    );
  }

  // Handle empty or 204 responses
  if (response.status === 204) {
    return null;
  }

  let data;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try {
      data = await response.json();
    } catch {
      data = null;
    }
  } else {
    const text = await response.text();
    data = { message: text };
  }

  if (!response.ok) {
    const message =
      (data && (data.message || data.error)) ||
      `HTTP request failed with status ${response.status} (${response.statusText})`;
    throw new ApiError(response.status, message, data || {});
  }

  return data;
}

export const api = {
  /**
   * Health check endpoint
   * GET /api/health
   */
  async checkHealth() {
    return request('/api/health', { method: 'GET' });
  },

  /**
   * Parse UPI QR string
   * POST /api/qr/parse
   * @param {string} qrText
   */
  async parseQr(qrText) {
    if (!qrText || typeof qrText !== 'string' || !qrText.trim()) {
      throw new ApiError(400, 'QR payload text cannot be empty.', { error: 'INVALID_REQUEST' });
    }
    return request('/api/qr/parse', {
      method: 'POST',
      body: JSON.stringify({ qrText: qrText.trim() }),
    });
  },

  /**
   * Evaluate payment route compatibility
   * POST /api/compatibility/evaluate
   * @param {string} qrText
   * @param {string} visitorCountry - 2-letter ISO code e.g. "DE", "US"
   * @param {string} candidateProvider - e.g. "paypal", "paytm", "phonepe", "gpay"
   */
  async evaluateCompatibility(qrText, visitorCountry, candidateProvider) {
    if (!qrText || !qrText.trim()) {
      throw new ApiError(400, 'QR payload text is required for evaluation.', { error: 'INVALID_REQUEST' });
    }
    if (!visitorCountry || !visitorCountry.trim()) {
      throw new ApiError(400, 'Visitor country ISO code (e.g. "US", "DE") is required.', { error: 'INVALID_REQUEST' });
    }
    if (!candidateProvider || !candidateProvider.trim()) {
      throw new ApiError(400, 'Candidate payment provider identifier is required.', { error: 'INVALID_REQUEST' });
    }

    return request('/api/compatibility/evaluate', {
      method: 'POST',
      body: JSON.stringify({
        qrText: qrText.trim(),
        visitorCountry: visitorCountry.trim().toUpperCase(),
        candidateProvider: candidateProvider.trim().toLowerCase(),
      }),
    });
  },

  /**
   * Request Gemini AI explanation for a given evaluation
   * POST /api/ai/explain
   * @param {string} evaluationId
   */
  async getAiExplanation(evaluationId) {
    if (!evaluationId || !evaluationId.trim()) {
      throw new ApiError(400, 'Evaluation ID is required to generate AI explanation.', { error: 'INVALID_REQUEST' });
    }
    return request('/api/ai/explain', {
      method: 'POST',
      body: JSON.stringify({ evaluationId: evaluationId.trim() }),
    });
  },

  /**
   * Retrieve all saved evaluations for dashboard & RouteIQ AG Grid
   * GET /api/evaluations
   */
  async getEvaluations() {
    return request('/api/evaluations', { method: 'GET' });
  },

  /**
   * Create PayPal Sandbox test order
   * POST /api/payments/orders
   * @param {string} amount - e.g. "5.00"
   * @param {string} currency - 3-letter ISO currency e.g. "USD"
   */
  async createPayPalOrder(amount, currency = 'USD') {
    if (!amount || !amount.toString().trim()) {
      throw new ApiError(400, 'Amount is required.', { error: 'INVALID_REQUEST' });
    }
    return request('/api/payments/orders', {
      method: 'POST',
      body: JSON.stringify({
        amount: amount.toString().trim(),
        currency: currency.trim().toUpperCase(),
      }),
    });
  },

  /**
   * Capture an approved PayPal Sandbox order
   * POST /api/payments/orders/{orderId}/capture
   * @param {string} orderId
   */
  async capturePayPalOrder(orderId) {
    if (!orderId || !orderId.trim()) {
      throw new ApiError(400, 'Order ID is required to capture payment.', { error: 'INVALID_REQUEST' });
    }
    return request(`/api/payments/orders/${encodeURIComponent(orderId.trim())}/capture`, {
      method: 'POST',
    });
  },
};

export default api;
