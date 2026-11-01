package com.backend.presentation.controller;

import com.backend.application.dto.response.chapter.ChapterResponse;
import com.backend.application.service.ChapterService;
import com.backend.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChapterControllerTest {

    @Mock
    private ChapterService chapterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChapterController(chapterService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createChapter_returnsTheCreatedChapter() throws Exception {
        when(chapterService.createChapter(eq(7L), any()))
                .thenReturn(ChapterResponse.builder().id(9L).title("Mở đầu").chapterOrder(2).build());

        mockMvc.perform(post("/books/7/chapters").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Mở đầu\", \"chapterOrder\": 2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.chapterOrder").value(2));
    }

    @Test
    void createChapter_requiresAnOrder() throws Exception {
        mockMvc.perform(post("/books/7/chapters").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Mở đầu\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chapterService);
    }

    @Test
    void createChapter_requiresATitle() throws Exception {
        mockMvc.perform(post("/books/7/chapters").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"   \", \"chapterOrder\": 2}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chapterService);
    }

    @Test
    void createChapter_rejectsANegativeOrder() throws Exception {
        mockMvc.perform(post("/books/7/chapters").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Mở đầu\", \"chapterOrder\": -1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chapterService);
    }

    @Test
    void update_rejectsANegativeOrder() throws Exception {
        mockMvc.perform(patch("/books/7/chapters/9").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chapterOrder\": -1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chapterService);
    }
}
