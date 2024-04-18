package be.uantwerpen.distributedclients.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("nodes")
public class NodeController {
    @GetMapping("{amount}")
    public void getAmountExistingNodes(@RequestParam int amount) {
    }

}
