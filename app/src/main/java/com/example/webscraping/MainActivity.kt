package com.example.webscraping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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


interface WebScrapingApi {
    @GET("/acoes/mult3/")
    fun getHtml(): Call<String>
}


object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://investidor10.com.br/")
            .addConverterFactory(ScalarsConverterFactory.create())
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
                MainContent()
            }
        }
    }
}

@Composable
fun MainContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Start()
        Spacer(modifier = Modifier.height(16.dp))
        PrintCotacao()
        Spacer(modifier = Modifier.height(16.dp))
        PrintDividendosHist()
    }
}

@Composable
fun Start() {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text("Segue Cotação e dividendos da MULT3")
            Spacer(modifier = Modifier.width(8.dp))
        }
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text("Fonte: Investidor 10")
            Spacer(modifier = Modifier.width(8.dp)) 
        }

    }
}

@Composable
fun PrintCotacao() {
    var value by remember { mutableStateOf("Carregando...") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val call = RetrofitInstance.api.getHtml()
            call.enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        response.body()?.let { html ->
                            val document = Jsoup.parse(html)
                            val cotacaoValue = document.select("div._card.cotacao div._card-body span.value").text()
                            value = cotacaoValue
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Cotação: $value")
    }
}

@Composable
fun PrintDividendosHist() {
    var dividendos by remember { mutableStateOf(listOf<List<String>>()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val call = RetrofitInstance.api.getHtml()
            call.enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        response.body()?.let { html ->
                            val document = Jsoup.parse(html)

                            val dividendosTable = document.select("div#dividends-section div.content.no_datatable.indicator-history table#table-dividends-history")
                            val rows = dividendosTable.select("tbody tr")
                            val dividendosList = mutableListOf<List<String>>()

                            for (row in rows) {
                                val columns = row.select("td")
                                if (columns.size >= 4) {
                                    val tipo = columns[0].text()
                                    val dataCom = columns[1].text()
                                    val pagamento = columns[2].text()
                                    val valor = columns[3].text()

                                    dividendosList.add(listOf(tipo, dataCom, pagamento, valor))
                                }
                            }

                            dividendos = dividendosList
                        }
                    } else {
                        dividendos = listOf(listOf("Falha ao obter dividendos", "", "", ""))
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    dividendos = listOf(listOf("Erro ao carregar dados", "", "", ""))
                }
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Tipo", modifier = Modifier.weight(1f))
            Text(text = "Data Com", modifier = Modifier.weight(1f))
            Text(text = "Pagamento", modifier = Modifier.weight(1f))
            Text(text = "Valor", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        for (dividendo in dividendos) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = dividendo[0], modifier = Modifier.weight(1f), fontSize = 12.sp)
                Text(text = dividendo[1], modifier = Modifier.weight(1f), fontSize = 12.sp)
                Text(text = dividendo[2], modifier = Modifier.weight(1f), fontSize = 12.sp)
                Text(text = dividendo[3], modifier = Modifier.weight(1f), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
