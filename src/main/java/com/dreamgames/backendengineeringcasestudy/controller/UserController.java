package com.dreamgames.backendengineeringcasestudy.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dreamgames.backendengineeringcasestudy.models.User;
import com.dreamgames.backendengineeringcasestudy.services.UserService;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/createUser")
    public ResponseEntity<User> createUser() {
        User user = userService.createUser();
        return ResponseEntity.ok(user);
    }

    @PutMapping("/updateLevel")
    public ResponseEntity<User> updateUserLevel(@RequestParam Long userId) {
        User updatedUser = userService.updateLevel(userId);
        return ResponseEntity.ok(updatedUser);
    }
}
