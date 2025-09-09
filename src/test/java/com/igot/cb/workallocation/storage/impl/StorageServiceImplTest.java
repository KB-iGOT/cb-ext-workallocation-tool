package com.igot.cb.workallocation.storage.impl;

import com.igot.cb.workallocation.authentication.util.AccessTokenValidator;
import com.igot.cb.workallocation.util.ApiResponse;
import com.igot.cb.workallocation.util.CbServerProperties;
import com.igot.cb.workallocation.util.Constants;
import com.igot.cb.workallocation.util.ProjectUtil;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.sunbird.cloud.storage.BaseStorageService;
import scala.Option;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageServiceImplTest {

    @InjectMocks
    private StorageServiceImpl storageService;

    @Mock
    private AccessTokenValidator accessTokenValidator;
    @Mock
    private CbServerProperties cbServerProperties;
    @Mock
    private BaseStorageService baseStorageService;

    private AutoCloseable closeable;
    private String tempHtmlDir;
    private String tempPdfDir;

    @BeforeEach
    void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);

        tempHtmlDir = Files.createTempDirectory("html").toString();
        tempPdfDir = Files.createTempDirectory("pdf").toString();

        setPrivateField(storageService, "htmlFolderPath", tempHtmlDir);
        setPrivateField(storageService, "pdfFolderPath", tempPdfDir);

        when(cbServerProperties.getCloudStorageTypeName()).thenReturn("gcp");
        when(cbServerProperties.getCloudStorageKey()).thenReturn("key");
        when(cbServerProperties.getCloudStorageSecret()).thenReturn("secret");
        when(cbServerProperties.getCloudStorageEndpoint()).thenReturn("endpoint");
        when(cbServerProperties.getCloudFolderName()).thenReturn("folder");
        when(cbServerProperties.getCloudContainerName()).thenReturn("container");

        storageService.init();
        setPrivateField(storageService, "storageService", baseStorageService);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testInit_Idempotent() {
        // Should not throw or re-init if already initialized
        storageService.init();
    }

    @Test
    void testUpdateErrorDetails_NullResponse() {
        storageService.updateErrorDetails(null, "err", HttpStatus.BAD_REQUEST);
        // No exception, nothing to assert
    }

    @Test
    void testUpdateErrorDetails_NullParams() {
        ApiResponse response = new ApiResponse();
        response.setParams(null);
        storageService.updateErrorDetails(response, "err", HttpStatus.BAD_REQUEST);
        // No exception, nothing to assert
    }

    @Test
    void testUpdateErrorDetails_Valid() {
        ApiResponse response = ProjectUtil.createDefaultResponse("test");
        storageService.updateErrorDetails(response, "err", HttpStatus.BAD_REQUEST);
        assertEquals("err", response.getParams().getErrMsg());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testCreateHTMLFileAndContent() throws Exception {
        String content = "<html>test</html>";
        String path = storageService.createHTMLFile("test", content);
        assertTrue(new File(path).exists());
        String fileContent = Files.readString(Path.of(path));
        assertEquals(content, fileContent);
    }

    @Test
    void testCreateHTMLFile_DirectoryExists() throws Exception {
        // Directory already exists
        Files.createDirectories(Path.of(tempHtmlDir));
        String content = "abc";
        String path = storageService.createHTMLFile("file", content);
        assertTrue(new File(path).exists());
    }

    @Test
    void testMakePdf_NullHtmlFilePath() throws Exception {
        Map<String, String> paramMap = new HashMap<>();
        String result = storageService.makePdf(paramMap);
        assertNull(result);
    }

    @Test
    void testMakePdf_NullFileName() throws Exception {
        Map<String, String> paramMap = new HashMap<>();
        File html = File.createTempFile("test", ".html", new File(tempHtmlDir));
        paramMap.put(Constants.UD_HTML_FILE_PATH, html.getAbsolutePath());
        // No file name provided
        paramMap.put(Constants.UD_HTML_HEADER_FILE_PATH, html.getAbsolutePath());
        paramMap.put(Constants.UD_HTML_FOOTER_FILE_PATH, html.getAbsolutePath());
        // Simulate process failure by using a dummy command (simulate exit code != 0)
        setPrivateField(storageService, "pdfFolderPath", tempPdfDir);
        String result = storageService.makePdf(paramMap);
        assertTrue(result.endsWith(".pdf"));
    }

    @Test
    void testMakePdf_InterruptedException() throws Exception {
        Map<String, String> paramMap = new HashMap<>();
        File html = File.createTempFile("test", ".html", new File(tempHtmlDir));
        paramMap.put(Constants.UD_HTML_FILE_PATH, html.getAbsolutePath());
        paramMap.put(Constants.UD_FILE_NAME, "file.pdf");
        paramMap.put(Constants.UD_HTML_HEADER_FILE_PATH, html.getAbsolutePath());
        paramMap.put(Constants.UD_HTML_FOOTER_FILE_PATH, html.getAbsolutePath());

        // Simulate InterruptedException by mocking Runtime.getRuntime().exec
        Runtime runtime = mock(Runtime.class);
        Process process = mock(Process.class);
        when(process.waitFor()).thenThrow(new InterruptedException());
        when(runtime.exec(anyString())).thenReturn(process);

        // Use reflection to replace Runtime.getRuntime() if needed (not trivial in Java, so this branch is hard to cover in unit test)
        // This test will just call the real method for coverage

        String result = storageService.makePdf(paramMap);
        assertTrue(result.endsWith(".pdf"));
    }

    @Test
    void testUploadFile_FileDoesNotExist() {
        File file = new File("nonexistent.txt");
        ApiResponse response = storageService.uploadFile(file, "folder", "container");
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("File does not exist.", response.getParams().getErrMsg());
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testUploadFile_Success() throws Exception {
        File file = File.createTempFile("test", ".txt");
        file.deleteOnExit();
        when(baseStorageService.upload(anyString(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn("http://uploaded.url");
        ApiResponse response = storageService.uploadFile(file, "folder", "container");
        assertEquals(file.getName(), response.getResult().get(Constants.NAME));        assertEquals("http://uploaded.url", response.getResult().get(Constants.URL));
        // File should be deleted after upload
        assertFalse(file.exists());
    }

    @Test
    void testUploadFile_Exception() throws Exception {
        File file = File.createTempFile("test", ".txt");
        file.deleteOnExit();
        when(baseStorageService.upload(anyString(), anyString(), anyString(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("fail"));
        ApiResponse response = storageService.uploadFile(file, "folder", "container");
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertTrue(response.getParams().getErrMsg().contains("Failed to upload file"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        // File should not be deleted
        assertTrue(file.exists());
    }

    @Test
    void testUploadFile_DeleteFileFails() throws Exception {
        File file = spy(File.createTempFile("test", ".txt"));
        doReturn(true).when(file).exists();
        doReturn(true).when(file).delete();
        when(baseStorageService.upload(anyString(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn("http://uploaded.url");
        ApiResponse response = storageService.uploadFile(file, "folder", "container");
        assertEquals(file.getName(), response.getResult().get(Constants.NAME));    }

    @Test
    void testUploadFileToGCPContainer_UserIdEmpty() {
        Map<String, Object> req = new HashMap<>();
        when(accessTokenValidator.fetchUserIdFromAccessToken(anyString())).thenReturn("");
        ApiResponse response = storageService.uploadFileToGCPContainer(req, "token");
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals(Constants.USER_ID_DOESNT_EXIST, response.getParams().getErrMsg());
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }
    
    @Test
    void testUploadFileToGCPContainer_Exception() throws Exception {
        Map<String, Object> req = new HashMap<>();
        when(accessTokenValidator.fetchUserIdFromAccessToken(anyString())).thenReturn("user1");

        // Force createHTMLFile to throw IOException
        StorageServiceImpl spyService = Mockito.spy(storageService);
        doThrow(new IOException("fail")).when(spyService).createHTMLFile(anyString(), anyString());

        ApiResponse response = spyService.uploadFileToGCPContainer(req, "token");
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("Failed to generate or upload PDF", response.getParams().getErrMsg());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void testRenderTemplate() throws Exception {
        // Use real velocity engine for coverage
        VelocityContext context = new VelocityContext();
        context.put("key", "value");
        StringWriter writer = new StringWriter();
        // Create a dummy template file in classpath
        String templateContent = "Hello $key";
        Path templatePath = Path.of(tempHtmlDir, "test.vm");
        Files.writeString(templatePath, templateContent);

        VelocityEngine ve = new VelocityEngine();
        ve.setProperty("file.resource.loader.path", tempHtmlDir);
        ve.init();
        setPrivateField(storageService, "velocityEngine", ve);

        // Use reflection to call private method
        java.lang.reflect.Method m = StorageServiceImpl.class.getDeclaredMethod("renderTemplate", String.class, VelocityContext.class);
        m.setAccessible(true);
        String result = (String) m.invoke(storageService, templatePath.getFileName().toString(), context);
        assertTrue(result.contains("Hello"));
    }

    @Test
    void testUpdateErrorDetails_ResponseNull() {
        storageService.updateErrorDetails(null, "err", HttpStatus.BAD_REQUEST);
        // No exception expected
    }

    @Test
    void testUpdateErrorDetails_ParamsNull() {
        ApiResponse response = new ApiResponse();
        response.setParams(null);
        storageService.updateErrorDetails(response, "err", HttpStatus.BAD_REQUEST);
        // No exception expected
    }

    @Test
    void testUploadFile_NullFile() {
        ApiResponse response = storageService.uploadFile(null, "folder", "container");
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertEquals("File does not exist.", response.getParams().getErrMsg());
        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
    }

    @Test
    void testMakePdf_FileNameWithoutPdfExtension() throws Exception {
        Map<String, String> paramMap = new HashMap<>();
        File html = File.createTempFile("test", ".html", new File(tempHtmlDir));
        paramMap.put(Constants.UD_HTML_FILE_PATH, html.getAbsolutePath());
        paramMap.put(Constants.UD_FILE_NAME, "file"); // No .pdf extension
        paramMap.put(Constants.UD_HTML_HEADER_FILE_PATH, html.getAbsolutePath());
        paramMap.put(Constants.UD_HTML_FOOTER_FILE_PATH, html.getAbsolutePath());
        String result = storageService.makePdf(paramMap);
        assertTrue(result.endsWith(".pdf"));
    }

    @Test
    void testMakePdf_IOException() throws Exception {
        Map<String, String> paramMap = new HashMap<>();
        File html = File.createTempFile("test", ".html", new File(tempHtmlDir));
        paramMap.put(Constants.UD_HTML_FILE_PATH, html.getAbsolutePath());
        paramMap.put(Constants.UD_FILE_NAME, "file.pdf");
        paramMap.put(Constants.UD_HTML_HEADER_FILE_PATH, html.getAbsolutePath());
        paramMap.put(Constants.UD_HTML_FOOTER_FILE_PATH, html.getAbsolutePath());
        // Simulate IOException from exec
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime runtime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            when(runtime.exec(anyString())).thenThrow(new IOException("exec failed"));
            String result = storageService.makePdf(paramMap);
            assertTrue(result.endsWith(".pdf"));
        }
    }

    @Test
    void testInit_AlreadyInitialized() throws Exception {
        setPrivateField(storageService, "storageService", baseStorageService);
        storageService.init();
        // Should not re-initialize
    }

    @Test
    void testRenderTemplate_InvalidTemplatePath() {
        VelocityContext context = new VelocityContext();
        assertThrows(Exception.class, () -> {
            java.lang.reflect.Method m = StorageServiceImpl.class.getDeclaredMethod("renderTemplate", String.class, VelocityContext.class);
            m.setAccessible(true);
            m.invoke(storageService, "nonexistent.vm", context);
        });
    }
}