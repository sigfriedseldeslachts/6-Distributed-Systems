package be.uantwerpen.distributedclients.services;

import be.uantwerpen.distributedclients.utils.HashingFunction;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

@Service
@EnableAsync
public class FileService {

    private ArrayList<Integer> fileList = new ArrayList<>();

    public void send(){
        RestClient client = RestClient.builder()
                .baseUrl("http://" + "127.0.0.1:8080" + "/files/replication")
                .defaultHeader("Content-Type", "application/json")
                .build();
        Map<Integer, Integer> map = client.method(HttpMethod.POST).body(fileList).retrieve().body(new ParameterizedTypeReference<>() {});
        System.out.println(map);
    }

    @Async
    @Scheduled(fixedRate = 5000)
    public void update(){
        File directory = new File("C:\\Users\\goran\\Desktop\\data");
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.add(HashingFunction.getHashFromString(file.getName()));
            }
        }
    }


}
