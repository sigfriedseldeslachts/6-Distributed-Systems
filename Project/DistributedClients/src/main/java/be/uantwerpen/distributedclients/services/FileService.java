package be.uantwerpen.distributedclients.services;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

@Service
public class FileService {

    private InfoService infoService;
    private ArrayList<Integer> fileList = new ArrayList<>();

    public FileService() {

        File directory = new File("");
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.add(file.getName().hashCode());
            }
        }
    }

    public void send(){
        RestClient client = RestClient.builder()
                .baseUrl("http://" + "127.0.0.1:8080" + "/files/replication")
                .defaultHeader("Content-Type", "application/json")
                .build();
        Map<Integer, Integer> map = client.method(HttpMethod.POST).body(fileList).retrieve().body(new ParameterizedTypeReference<>() {});
        System.out.println(map);
    }


}
