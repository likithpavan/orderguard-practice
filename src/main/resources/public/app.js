'use strict';
const form = document.querySelector('#order-form');
const submitButton = document.querySelector('#submit-order');
const decision = document.querySelector('#decision');
const decisionTitle = document.querySelector('#decision-title');
const decisionMessage = document.querySelector('#decision-message');
const requestId = document.querySelector('#request-id');
const clearButton = document.querySelector('#clear-orders');

function showDecision(kind, title, message, id) {
  decision.className = `decision ${kind}`;
  decisionTitle.textContent = title;
  decisionMessage.textContent = message;
  requestId.textContent = id ? `Request: ${id}` : '';
  requestId.hidden = !id;
}

async function refreshOrders() {
  const error = document.querySelector('#ledger-error');
  try {
    const response = await fetch('/api/orders');
    if (!response.ok) throw new Error('Could not load accepted orders. Check the server.');
    const data = await response.json();
    document.querySelector('#order-count').textContent = String(data.count);
    const body = document.querySelector('#orders-body');
    body.replaceChildren();
    for (const order of [...data.orders].reverse()) {
      const row = document.createElement('tr');
      const values = [order.symbol, order.side, order.quantity.toLocaleString(), new Date(order.createdAt).toLocaleTimeString()];
      for (const [index, value] of values.entries()) {
        const cell = document.createElement('td');
        cell.textContent = value;
        if (index === 1) cell.className = `side-${order.side.toLowerCase()}`;
        row.append(cell);
      }
      body.append(row);
    }
    document.querySelector('#empty-state').hidden = data.count !== 0;
    clearButton.disabled = data.count === 0;
    error.hidden = true;
  } catch (exception) {
    error.textContent = exception.message;
    error.hidden = false;
  }
}

form.addEventListener('submit', async event => {
  event.preventDefault();
  submitButton.disabled = true;
  try {
    const response = await fetch('/api/orders', {method: 'POST', body: new URLSearchParams(new FormData(form))});
    const data = await response.json();
    const id = response.headers.get('X-Request-Id');
    if (response.status === 201) {
      showDecision('accepted', 'Order accepted · HTTP 201', `${data.order.side} ${data.order.quantity.toLocaleString()} ${data.order.symbol} shares. One order added to the ledger.`, id);
    } else {
      showDecision('rejected', `Order not accepted · HTTP ${response.status}`, data.message, id);
    }
  } catch (exception) {
    showDecision('rejected', 'Response not confirmed', 'Check the server and refresh the ledger before retrying. A lost response does not prove the order was rejected.', null);
  } finally {
    await refreshOrders();
    submitButton.disabled = false;
  }
});

document.querySelector('#refresh-orders').addEventListener('click', refreshOrders);
clearButton.addEventListener('click', async () => {
  if (!window.confirm('Clear all simulated orders in this environment?')) return;
  clearButton.disabled = true;
  try {
    const response = await fetch('/api/orders', {method: 'DELETE'});
    const data = await response.json();
    if (!response.ok) throw new Error(data.message);
    showDecision('neutral', 'Practice ledger cleared', 'Accepted-order count is now zero.', null);
  } catch (exception) {
    showDecision('rejected', 'Could not clear the ledger', exception.message, null);
  } finally { await refreshOrders(); }
});

async function loadBuildInfo() {
  try {
    const response = await fetch('/api/health');
    if (!response.ok) throw new Error('Server unavailable');
    const data = await response.json();
    document.querySelector('#environment').textContent = `${data.environment} · ${data.status}`;
    document.querySelector('#build-info').textContent = `v${data.version} · commit ${data.commit.slice(0, 8)}`;
    document.querySelector('#starter-notice').hidden = data.version !== '0.1.0';
  } catch (exception) {
    document.querySelector('#environment').textContent = 'Server unavailable';
    document.querySelector('#build-info').textContent = 'Start the Java server to connect.';
  }
}
loadBuildInfo();
refreshOrders();
