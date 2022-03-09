package org.stellar.anchor.platform.controller

import org.mockito.BDDMockito.*
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest

@WebMvcTest(Sep38Controller::class)
class Sep38ControllerTest {
  // Temporarily comment this out to merge with the new integration test module.

  //  @Autowired private lateinit var mockMvc: MockMvc
  //
  //  @MockBean private lateinit var sep38Service: Sep38Service
  //
  //  @BeforeEach
  //  fun setUp() {
  //    // we set the result of the mocked service
  //    given(sep38Service.info)
  //      .willReturn(InfoResponse(ResourceJsonAssetService("test_assets.json").listAllAssets()))
  //  }
  //
  //  @Test
  //  @Throws(Exception::class)
  //  fun getInfo() {
  //    val wantResponse = FileUtil.getResourceFileAsString("test_sep38_get_info.json")
  //
  //    mockMvc
  //      .perform(MockMvcRequestBuilders.get("/sep38/info").accept(MediaType.APPLICATION_JSON))
  //      .andExpect(MockMvcResultMatchers.status().isOk)
  //      .andExpect(MockMvcResultMatchers.content().json(wantResponse))
  //  }
}
