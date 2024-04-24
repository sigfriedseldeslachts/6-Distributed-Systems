package be.uantwerpen.distributedclients.controllers;

import be.uantwerpen.distributedclients.services.InfoService;
import org.springframework.web.bind.annotation.*;

@RestController("/nodes")
public class NodeController {

    private InfoService infoservice;

    @PostMapping("{amount}")
    public void AmountExistingNodes(@RequestParam int amount) {
        this.infoservice.setAmountOfNodes(amount);
    }

    @PostMapping("/next")
    public void CurrentAndNext(@RequestBody int currentID,@RequestBody int nextID) {
        this.infoservice.setPreviousID(currentID);
        this.infoservice.setNextID(nextID);
    }

    @PostMapping("/previous")
    public void CurrentAndPrevious(@RequestBody int currentID,@RequestBody int previousID) {
        this.infoservice.setNextID(currentID);
        this.infoservice.setPreviousID(previousID);
    }
}
