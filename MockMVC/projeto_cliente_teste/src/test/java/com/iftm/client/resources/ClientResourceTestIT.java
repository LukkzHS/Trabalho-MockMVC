package com.iftm.client.resources;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iftm.client.dto.ClientDTO;

@SpringBootTest
@AutoConfigureMockMvc
public class ClientResourceTestIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long existingId;
    private Long nonExistingId;
    private ClientDTO clientDTO;

    @BeforeEach
    public void setUp() throws Exception {
        // INSERIR CLIENTE PARA TESTES
        clientDTO = new ClientDTO(null, "Lucca Henrique", "12345678900", 5000.0,
                Instant.parse("2003-08-20T07:50:00Z"), 1);
        String json = objectMapper.writeValueAsString(clientDTO);

        // INSERE E CAPTURA ID DO CLIENTE
        String response = mockMvc.perform(post("/clients/")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // EXTRAIR ID CRIADO
        existingId = objectMapper.readTree(response).path("id").asLong();
        nonExistingId = 999L; // ID QUE NAO EXISTE
    }

    @Test
    public void findAll_ShouldReturnAllClients() throws Exception {
        mockMvc.perform(get("/clients")
                .param("page", "0")
                .param("linesPerPage", "12")
                .param("direction", "ASC")
                .param("orderBy", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].name").exists());
    }

    @Test
    public void findById_ShouldReturnClient_WhenIdExists() throws Exception {
        mockMvc.perform(get("/clients/id/{id}", existingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingId))
                .andExpect(jsonPath("$.name").value("Lucca Henrique"));
    }

    @Test
    public void findById_ShouldReturnNotFound_WhenIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/clients/id/{id}", nonExistingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value("Entity not found"))
                .andExpect(jsonPath("$.path").value("/clients/id/" + nonExistingId));
    }

    @Test
    public void findByIncome_ShouldReturnClients_WhenIncomeExists() throws Exception {
        double income = 5000.0;

        mockMvc.perform(get("/clients/income/")
                .param("income", String.valueOf(income)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].income").value(income));
    }

    @Test
    public void findByIncomeGreaterThan_ShouldReturnClients_WhenIncomeIsGreaterThanValue() throws Exception {
        double income = 3000.0;

        mockMvc.perform(get("/clients/incomeGreaterThan/")
                .param("income", String.valueOf(income)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].income").value(greaterThan(income)));
    }

    @Test
    public void findByCPFLike_ShouldReturnClients_WhenCpfMatchesPattern() throws Exception {
        clientDTO = new ClientDTO(null, "Teste CPF", "10919444522", 3000.0,
                Instant.parse("1990-01-01T00:00:00Z"), 0);
        String json = objectMapper.writeValueAsString(clientDTO);

        mockMvc.perform(post("/clients/")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        String cpf = "109194445";
        mockMvc.perform(get("/clients/cpf/")
                .param("cpf", cpf))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.content[0].cpf").value(startsWith(cpf)));
    }

    @Test
    public void insert_ShouldCreateClient_WhenDataIsValid() throws Exception {
        ClientDTO clientDTO = new ClientDTO(null, "InsereCliente", "12345678900", 5000.0,
                Instant.parse("1985-10-20T07:50:00Z"), 1);
        String json = objectMapper.writeValueAsString(clientDTO);

        mockMvc.perform(post("/clients/")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("InsereCliente"));
    }

    @Test
    public void delete_ShouldReturnNoContent_WhenIdExists() throws Exception {
        mockMvc.perform(delete("/clients/{id}", existingId))
                .andExpect(status().isNoContent());
    }

    @Test
    public void delete_ShouldReturnNotFound_WhenIdDoesNotExist() throws Exception {
        mockMvc.perform(delete("/clients/{id}", nonExistingId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void update_ShouldReturnUpdatedClient_WhenIdExists() throws Exception {
        ClientDTO clientDTO = new ClientDTO(null, "Atualiza Cliente", "12345678900", 5000.0,
                Instant.parse("1985-10-20T07:50:00Z"), 1);
        String json = objectMapper.writeValueAsString(clientDTO);

        mockMvc.perform(put("/clients/{id}", existingId)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Atualiza Cliente"));
    }

    @Test
    public void update_ShouldReturnNotFound_WhenIdDoesNotExist() throws Exception {
        ClientDTO clientDTO = new ClientDTO(null, "Non-Existent", "12345678900", 5000.0,
                Instant.parse("1985-10-20T07:50:00Z"), 1);
        String json = objectMapper.writeValueAsString(clientDTO);

        mockMvc.perform(put("/clients/{id}", nonExistingId)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource not found"));
    }
}
