package com.example.webscraping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.webscraping.ui.theme.WebScrapingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Retrofit Interface
interface WebScrapingApi {
    @GET("/acoes/mult3/")
    fun getHtml(): Call<String>
}

// Retrofit Instance (Singleton)
object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://investidor10.com.br/")
            .addConverterFactory(ScalarsConverterFactory.create()) // Converter HTML para String
            .build()
    }

    val api: WebScrapingApi by lazy {
        retrofit.create(WebScrapingApi::class.java)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WebScrapingTheme {
                PrintCotacao()
                PrintDividendosHist()
            }
        }
    }
}

@Composable
fun PrintCotacao() {
    // Estado inicial como "Carregando..."
    var value by remember { mutableStateOf("Carregando...") }

    // LaunchedEffect para fazer o scraping ao iniciar a composição
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val call = RetrofitInstance.api.getHtml()
            call.enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        response.body()?.let { html ->
                            // Usando Jsoup para analisar o HTML
                            val document = Jsoup.parse(html)
                            // Seletor atualizado para pegar o valor dentro do span.value
                            val cotacaoValue = document.select("div._card.cotacao div._card-body span.value").text()
                            value = cotacaoValue  // Atualiza o valor
                        }
                    } else {
                        value = "Falha ao obter a cotação"
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    value = "Erro: ${t.message}"
                }
            })
        }
    }

    // Exibição do valor recuperado
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Cotação: $value")
    }
}

@Composable
fun PrintDividendosHist() {
    // Estado inicial como uma lista vazia
    var dividendos by remember { mutableStateOf(listOf<Pair<String, String>>()) } // Armazenar Tipo e Valor

    // LaunchedEffect para fazer o scraping ao iniciar a composição
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val call = RetrofitInstance.api.getHtml()
            call.enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        response.body()?.let { html ->
                            val document = Jsoup.parse(html)

                            // Seletor para pegar a tabela
                            val dividendosTable = document.select("div#dividends-section div.content.no_datatable.indicator-history table#table-dividends-history")

                            // Pegar as linhas da tabela
                            val rows = dividendosTable.select("tbody tr")
                            val dividendosList = mutableListOf<Pair<String, String>>() // Lista para armazenar os dividendos

                            // Iterar sobre as linhas
                            for (row in rows) {
                                val columns = row.select("td") // Pegar as colunas da linha
                                if (columns.size >= 4) { // Verifica se a linha tem pelo menos 4 colunas
                                    val tipo = columns[0].text() // Tipo
                                    val dataCom = columns[1].text() // Data com
                                    val pagamento = columns[2].text() // Pagamento
                                    val valor = columns[3].text() // Valor

                                    // Adicionar os dados à lista
                                    dividendosList.add(Pair(tipo, valor))
                                }
                            }

                            dividendos = dividendosList // Atualizar o estado com a lista de dividendos
                        }
                    } else {
                        dividendos = listOf("Falha ao obter a cotação" to "")
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    dividendos = listOf("Erro" to t.message.orEmpty())
                }
            })
        }
    }

    // Exibição dos valores recuperados
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Exibir cada par de tipo e valor
        for ((tipo, valor) in dividendos) {
            Text(text = "Tipo: $tipo, Valor: $valor")
        }
    }
}


