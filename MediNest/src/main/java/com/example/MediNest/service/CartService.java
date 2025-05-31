package com.example.MediNest.service;

import com.example.MediNest.entity.Cart;
import com.example.MediNest.entity.Product;
import com.example.MediNest.entity.User;
import com.example.MediNest.exceptions.DataNotFoundException;
import com.example.MediNest.model.AddCartModel;
import com.example.MediNest.model.CartModel;
import com.example.MediNest.repository.CartRepository;
import com.example.MediNest.repository.ProductRepository;
import com.example.MediNest.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final UserRepository userRepository;

    private final ProductRepository productRepository;

    private final CartRepository cartRepository;

    @Transactional
    public CartModel addItemToCart(String email, String productId) {
        User userById = userRepository.findByEmail(email);

        Product productById = productRepository.findById(productId)
                .orElseThrow(() -> new DataNotFoundException("Product Id Not Found"));

        Optional<Cart> optionalCart = cartRepository.findByUserUserIdAndProductProductId(userById.getUserId(), productId);

        Cart cart;
        if (optionalCart.isPresent()) {
            cart = optionalCart.get();
            cart.setQuantity(cart.getQuantity() + 1);
        } else {
            cart = new Cart();
            cart.setUser(userById);
            cart.setProduct(productById);
            cart.setQuantity(1);
        }

        Cart savedCart = cartRepository.save(cart);

        CartModel cartModel = new CartModel();
        cartModel.setUsername(userById.getUsername());
        cartModel.setProductName(productById.getProductName());
        cartModel.setProductPrice(productById.getProductPrice());
        cartModel.setQuantity(savedCart.getQuantity());

        return cartModel;
    }


    public void deleteItemById(String cartId) {
        Cart cartItemToRemove = cartRepository.findById(cartId)
                .orElseThrow(() -> new DataNotFoundException("Cart Item Already Removed"));

        int quantity = cartItemToRemove.getQuantity();
        if (quantity > 1) {
            cartItemToRemove.setQuantity(quantity - 1);
            cartRepository.save(cartItemToRemove);
        }
        else{
            cartRepository.delete(cartItemToRemove);
        }
    }


    public AddCartModel getCartDetails(String email) {
        User user = userRepository.findByEmail(email);

        List<Cart> cartList = cartRepository.findByUserUserId(user.getUserId());

        List<CartModel> cartModelList = cartList.stream().map(cart -> {
            CartModel cartModel = new CartModel();
            cartModel.setUsername(user.getUsername());
            cartModel.setProductName(cart.getProduct().getProductName());
            cartModel.setProductPrice(cart.getProduct().getProductPrice());
            cartModel.setQuantity(cart.getQuantity());
            return cartModel;
        }).toList();

        AddCartModel addCartModel = new AddCartModel();
        addCartModel.setUsername(user.getUsername());
        addCartModel.setCartModel(cartModelList);

        return addCartModel;
    }

}
