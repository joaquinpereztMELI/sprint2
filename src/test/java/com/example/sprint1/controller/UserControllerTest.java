package com.example.sprint1.controller;

import com.example.sprint1.dto.CountFollowersUserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getFollowersCountHappyPath() throws Exception {
        CountFollowersUserDto = new CountFollowersUserDto();
        String id = "2";
        String path = "/users/" + id + "/followers/count";
        this.mockMvc.perform(get(path))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
