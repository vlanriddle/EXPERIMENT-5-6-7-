package com.example.social.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-h2")
class AuthAndPostIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rotatesRefreshTokensAndRestrictsPostsToTheirOwner() throws Exception {
        String registerJson = "{\"email\":\"owner@example.test\",\"displayName\":\"Post Owner\","
                + "\"password\":\"test-password-12345\"}";
        MvcResult register = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode session = objectMapper.readTree(register.getResponse().getContentAsString()).path("data");
        String ownerAccessToken = session.path("accessToken").asText();
        Cookie firstRefreshCookie = register.getResponse().getCookie("refresh_token");

        MvcResult created = mvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + ownerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"A protected post\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-ID"))
                .andReturn();
        String postId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asText();

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(firstRefreshCookie))
                .andExpect(status().isOk())
                .andReturn();
        Cookie rotatedCookie = refreshed.getResponse().getCookie("refresh_token");
        String newAccessToken = objectMapper.readTree(refreshed.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        org.junit.jupiter.api.Assertions.assertNotEquals(firstRefreshCookie.getValue(), rotatedCookie.getValue());

        mvc.perform(post("/api/auth/refresh").cookie(firstRefreshCookie))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/posts?page=0&size=10&sortBy=createdAt&sortDir=desc")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.pageNumber").value(0));

        String anotherUser = "{\"email\":\"other@example.test\",\"displayName\":\"Other User\","
                + "\"password\":\"test-password-12345\"}";
        MvcResult otherSession = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(anotherUser))
                .andExpect(status().isCreated())
                .andReturn();
        String otherToken = objectMapper.readTree(otherSession.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        mvc.perform(get("/api/posts/{id}", postId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/posts/{id}", postId)
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Updated protected post\",\"status\":\"DRAFT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
        mvc.perform(delete("/api/posts/{id}", postId).header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/posts/{id}", postId).header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isForbidden());
    }
}
