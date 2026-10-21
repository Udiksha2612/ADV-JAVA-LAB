package com.shopsphere.controller;

import com.shopsphere.dao.OrderDAO;
import com.shopsphere.dao.ProductDAO;
import com.shopsphere.model.CartItem;
import com.shopsphere.model.Order;
import com.shopsphere.model.Product;
import com.shopsphere.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CART_CHANGED_MESSAGE =
            "Some items in your cart changed (price, stock or availability). Please review your cart before checkout.";
    private OrderDAO orderDAO;
    private ProductDAO productDAO;

    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
        productDAO = new ProductDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            session.setAttribute("redirectAfterLogin", request.getContextPath() + "/checkout");
            session.setAttribute("flashError", "Please sign in to complete your checkout.");
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        @SuppressWarnings("unchecked")
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            session.setAttribute("flashError", "Your shopping cart is empty.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        if (refreshCart(cart)) {
            session.setAttribute("flashError", CART_CHANGED_MESSAGE);
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        double totalAmount = calculateCartTotal(cart);
        request.setAttribute("totalAmount", totalAmount);
        request.getRequestDispatcher("/checkout.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        @SuppressWarnings("unchecked")
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        String addressLine = request.getParameter("addressLine");
        String city = request.getParameter("city");
        String state = request.getParameter("state");
        String pincode = request.getParameter("pincode");
        String paymentMethod = request.getParameter("paymentMethod");

        if (addressLine == null || addressLine.trim().isEmpty() ||
            city == null || city.trim().isEmpty() ||
            state == null || state.trim().isEmpty() ||
            pincode == null || pincode.trim().isEmpty()) {
            request.setAttribute("error", "Please provide complete shipping address details.");
            request.setAttribute("totalAmount", calculateCartTotal(cart));
            request.getRequestDispatcher("/checkout.jsp").forward(request, response);
            return;
        }

        // Prices/stock in the session cart may be stale; never charge for an item the customer hasn't reviewed
        if (refreshCart(cart)) {
            session.setAttribute("flashError", CART_CHANGED_MESSAGE);
            response.sendRedirect(request.getContextPath() + "/cart");
            return;
        }

        String fullAddress = addressLine.trim() + ", " + city.trim() + ", " + state.trim() + " - " + pincode.trim();
        double serverCalculatedTotal = calculateCartTotal(cart);

        Order newOrder = new Order();
        newOrder.setUserId(user.getUserId());
        newOrder.setShippingAddress(fullAddress);
        newOrder.setTotalAmount(serverCalculatedTotal);
        newOrder.setDiscount(0.0);
        newOrder.setPaymentMethod(paymentMethod != null ? paymentMethod : "Cash On Delivery");
        newOrder.setPaymentStatus("Completed");
        newOrder.setOrderStatus("Processing");

        boolean placed = orderDAO.placeOrder(newOrder, cart);

        if (placed) {
            // Clear cart from session upon successful purchase
            cart.clear();
            session.setAttribute("cart", cart);
            session.setAttribute("latestOrder", newOrder);
            response.sendRedirect(request.getContextPath() + "/order-success.jsp?orderId=" + newOrder.getOrderId());
        } else {
            request.setAttribute("error", "Failed to place order. A selected item may have insufficient stock.");
            request.setAttribute("totalAmount", serverCalculatedTotal);
            request.getRequestDispatcher("/checkout.jsp").forward(request, response);
        }
    }

    /**
     * Reloads each cart product from the database so the order uses current prices and stock.
     * Drops deactivated or sold-out items and caps quantities. Returns true if anything changed.
     */
    private boolean refreshCart(List<CartItem> cart) {
        boolean changed = false;
        Iterator<CartItem> it = cart.iterator();
        while (it.hasNext()) {
            CartItem item = it.next();
            Product fresh = productDAO.getProductById(item.getProduct().getProductId());

            if (fresh == null || !"ACTIVE".equals(fresh.getStatus()) || fresh.getStock() <= 0) {
                it.remove();
                changed = true;
                continue;
            }
            if (item.getQuantity() > fresh.getStock()) {
                item.setQuantity(fresh.getStock());
                changed = true;
            }
            if (Double.compare(fresh.getEffectivePrice(), item.getProduct().getEffectivePrice()) != 0) {
                changed = true;
            }
            item.setProduct(fresh);
        }
        return changed;
    }

    private double calculateCartTotal(List<CartItem> cart) {
        double sum = 0.0;
        if (cart != null) {
            for (CartItem item : cart) {
                sum += item.getTotalPrice();
            }
        }
        return sum;
    }
}
