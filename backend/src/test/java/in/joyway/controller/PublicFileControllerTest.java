package in.joyway.controller;

import in.joyway.exception.GlobalExceptionHandler;
import in.joyway.service.storage.DocumentStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicFileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PublicFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentStorageService documentStorageService;

    @MockitoBean
    private in.joyway.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private in.joyway.security.JwtTokenProvider jwtTokenProvider;

    @Test
    void serveFile_returnsImageFromStorage() throws Exception {
        when(documentStorageService.retrieve("package-images/photo.jpg"))
                .thenReturn(Optional.of(new ByteArrayInputStream(new byte[]{(byte) 0xFF, (byte) 0xD8})));

        mockMvc.perform(get("/api/files/package-images/photo.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"));
    }

    @Test
    void serveFile_rejectsPathTraversalInFilename() throws Exception {
        mockMvc.perform(get("/api/files/package-images/..%2Fsecret.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void serveFile_rejectsNonPublicCategory() throws Exception {
        mockMvc.perform(get("/api/files/traveller-documents/passport.pdf"))
                .andExpect(status().isNotFound());
    }
}
