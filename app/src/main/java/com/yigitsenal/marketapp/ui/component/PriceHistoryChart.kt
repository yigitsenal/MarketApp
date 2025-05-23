package com.yigitsenal.marketapp.ui.component

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.yigitsenal.marketapp.data.model.PriceHistory
import com.yigitsenal.marketapp.ui.theme.PrimaryColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PriceHistoryChart(
    priceHistory: List<PriceHistory>,
    timeRange: TimeRange = TimeRange.MONTH,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = PrimaryColor.hashCode()
    
    // Debug için priceHistory'yi yazdır
    println("DEBUG: Orijinal veri sayısı: ${priceHistory.size}")
    priceHistory.forEach { 
        println("DEBUG: Orijinal veri: ${it.date} - ${it.price}")
    }
    
    // Filtrele ve sırala
    val filteredPriceHistory = remember(priceHistory, timeRange) {
        // Veri yoksa boş liste döndür
        if (priceHistory.isEmpty()) {
            println("DEBUG: Orijinal veri boş, filtreleme yapılmadı")
            return@remember emptyList()
        }
        
        when (timeRange) {
            TimeRange.WEEK -> {
                // Haftalık görünüm için son 7 günü al
                val result = filterPriceHistoryByTimeRange(priceHistory, timeRange).sortedBy { it.date }
                println("DEBUG: Haftalık filtrelenmiş veri sayısı: ${result.size}")
                result.forEach { 
                    println("DEBUG: Haftalık filtrelenmiş veri: ${it.date} - ${it.price}")
                }
                result
            }
            else -> {
                val result = filterPriceHistoryByTimeRange(priceHistory, timeRange).sortedBy { it.date }
                println("DEBUG: Filtrelenmiş veri sayısı: ${result.size}")
                result.forEach { 
                    println("DEBUG: Filtrelenmiş veri: ${it.date} - ${it.price}")
                }
                result
            }
        }
    }

    // X ekseni için tarih etiketleri
    val dateLabels = remember(filteredPriceHistory) {
        val labels = filteredPriceHistory.map { formatDateForXAxis(it.date, timeRange) }
        println("DEBUG: Etiket sayısı: ${labels.size}")
        labels.forEachIndexed { index, label -> 
            println("DEBUG: Etiket $index: $label")
        }
        labels
    }
    
    // Etiket sayısını kontrol et
    if (dateLabels.isEmpty()) {
        println("UYARI: Hiç etiket yok, grafik çizilemeyecek")
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setDrawGridBackground(false)
                isDragEnabled = true
                setScaleEnabled(false)
                setPinchZoom(false)
                
                // X ekseni ayarları
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)
                
                // Etiketlerin birbirine girmesini önlemek için ayarlar
                xAxis.labelRotationAngle = -45f // Etiketleri döndür
                
                // Zaman aralığına göre etiket sayısını ayarla
                when (timeRange) {
                    TimeRange.WEEK -> {
                        xAxis.labelCount = 7 // Haftalık görünümde her gün için etiket
                        xAxis.setLabelCount(7, true) // Tam olarak 7 etiket göster
                    }
                    TimeRange.MONTH -> {
                        // Aylık görünümde etiketlerin üst üste binmesini önlemek için daha az etiket göster
                        val labelCount = minOf(dateLabels.size, 6) // Aylık görünümde 6 etiket (daha az etiket, daha az çakışma)
                        xAxis.labelCount = labelCount
                        xAxis.setLabelCount(labelCount, true)
                    }
                    TimeRange.YEAR -> {
                        val labelCount = minOf(dateLabels.size, 6) // Yıllık görünümde 6 etiket
                        xAxis.labelCount = labelCount
                        xAxis.setLabelCount(labelCount, true)
                    }
                }
                
                // Etiketlerin daha iyi görünmesi için ek ayarlar
                xAxis.textSize = 8f // Daha küçük yazı boyutu
                xAxis.yOffset = 10f // Etiketleri biraz aşağı kaydır
                
                // Y ekseni ayarları
                axisLeft.setDrawGridLines(true)
                // Y ekseni minimum değerini dinamik olarak ayarla (0 yerine)
                // Grafik daha anlamlı bir aralıkta gösterilecek
                axisRight.isEnabled = false
                
                legend.isEnabled = true
                
                // Ekstra boşluk ekle
                extraBottomOffset = 16f
                extraLeftOffset = 8f
                extraRightOffset = 8f
            }
        },
        update = { chart ->
            val entries = filteredPriceHistory.mapIndexed { index, pricePoint ->
                Entry(index.toFloat(), pricePoint.price.toFloat())
            }
            
            // Veri setindeki minimum fiyatı bul
            val minPrice = if (entries.isNotEmpty()) {
                val minY = entries.minByOrNull { it.y }?.y ?: 0f
                // Minimum fiyattan biraz daha düşük bir değer kullan (grafiğin alt kısmında boşluk bırakmak için)
                // Minimum fiyatın %15 altı veya en az 10 birim altı
                val offset = minOf(minY * 0.15f, 10f)
                maxOf(minY - offset, 0f) // Negatif olmamasını sağla
            } else {
                0f
            }
            
            // Y ekseni minimum değerini ayarla
            chart.axisLeft.axisMinimum = minPrice
            println("DEBUG: Y ekseni minimum değeri: $minPrice")

            println("DEBUG: Entry sayısı: ${entries.size}")
            entries.forEachIndexed { index, entry -> 
                println("DEBUG: Entry $index: x=${entry.x}, y=${entry.y}")
            }

            val dataSet = LineDataSet(entries, "Fiyat (₺)").apply {
                color = primaryColor
                setCircleColor(primaryColor)
                lineWidth = 2f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawFilled(true)
                fillColor = primaryColor
                fillAlpha = 30
                mode = LineDataSet.Mode.CUBIC_BEZIER
                
                // Değer etiketlerini özelleştir
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        // Zaman aralığına göre değer etiketlerini göster/gizle
                        return when (timeRange) {
                            TimeRange.WEEK -> "₺${value.toInt()}" // Haftalık görünümde tüm değerleri göster
                            else -> {
                                // Diğer görünümlerde seçici olarak göster
                                // Veri noktası sayısına göre etiketleri azalt
                                if (entries.size > 10) {
                                    val index = entries.indexOfFirst { it.y == value }
                                    if (index % 3 != 0) "" else "₺${value.toInt()}"
                                } else {
                                    "₺${value.toInt()}"
                                }
                            }
                        }
                    }
                }
            }

            val lineData = LineData(dataSet)
            chart.data = lineData
            
            // Animasyon ekle
            chart.animateX(500)
            
            chart.invalidate() // Grafiği yeniden çiz
        }
    )
}

// Tarih aralığına göre fiyat geçmişini filtrele
private fun filterPriceHistoryByTimeRange(
    priceHistory: List<PriceHistory>, 
    timeRange: TimeRange
): List<PriceHistory> {
    if (priceHistory.isEmpty()) return emptyList()
    
    // Bugünün tarihini al
    val currentCalendar = Calendar.getInstance()
    // Gün sonuna ayarla
    currentCalendar.set(Calendar.HOUR_OF_DAY, 23)
    currentCalendar.set(Calendar.MINUTE, 59)
    currentCalendar.set(Calendar.SECOND, 59)
    currentCalendar.set(Calendar.MILLISECOND, 999)
    val today = currentCalendar.time
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    println("DEBUG: Bugünün tarihi: ${dateFormat.format(today)}")
    
    // Başlangıç tarihini hesapla
    val startCalendar = Calendar.getInstance()
    startCalendar.time = today
    
    when (timeRange) {
        TimeRange.WEEK -> {
            // Son 7 günü göster (bugün dahil)
            startCalendar.add(Calendar.DAY_OF_YEAR, -6)
        }
        TimeRange.MONTH -> {
            // Son 30 günü göster (bugün dahil)
            startCalendar.add(Calendar.DAY_OF_YEAR, -29)
        }
        TimeRange.YEAR -> {
            // Son 365 günü göster (bugün dahil)
            startCalendar.add(Calendar.DAY_OF_YEAR, -364)
        }
    }
    
    // Başlangıç tarihini günün başlangıcına ayarla
    startCalendar.set(Calendar.HOUR_OF_DAY, 0)
    startCalendar.set(Calendar.MINUTE, 0)
    startCalendar.set(Calendar.SECOND, 0)
    startCalendar.set(Calendar.MILLISECOND, 0)
    val startDate = startCalendar.time
    
    // Debug için tarihleri yazdır
    println("DEBUG: Bugün: ${dateFormat.format(today)}")
    println("DEBUG: Başlangıç: ${dateFormat.format(startDate)}")
    
    // Haftalık görünüm için özel işlem
    if (timeRange == TimeRange.WEEK) {
        // Son 7 günün her biri için bir veri noktası oluştur
        val resultMap = mutableMapOf<String, PriceHistory>()
        
        // Son 7 günün tarihlerini oluştur
        val dates = mutableListOf<String>()
        val tempCalendar = Calendar.getInstance()
        tempCalendar.time = startDate
        
        println("DEBUG: Haftalık görünüm başlangıç tarihi: ${dateFormat.format(startDate)}")
        
        for (i in 0 until 7) {
            val dateStr = dateFormat.format(tempCalendar.time)
            dates.add(dateStr)
            println("DEBUG: Haftalık tarih eklendi: $dateStr")
            tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // Her tarih için en son fiyatı bul
        dates.forEach { date ->
            val pricesForDate = priceHistory.filter { it.date == date }
            if (pricesForDate.isNotEmpty()) {
                // O gün için birden fazla veri varsa en son kaydedileni al
                resultMap[date] = pricesForDate.maxByOrNull { it.date } ?: pricesForDate.first()
                println("DEBUG: Tarih için veri bulundu: $date")
            } else {
                println("DEBUG: Tarih için veri bulunamadı: $date")
                try {
                    val dateObj = dateFormat.parse(date)
                    // Eğer o gün için veri yoksa, en yakın önceki günün verisini bul
                    val closestPreviousPrice = priceHistory
                        .filter { priceDate -> 
                            val pDate = dateFormat.parse(priceDate.date)
                            pDate != null && pDate.before(dateObj)
                        }
                        .maxByOrNull { it.date }
                    
                    if (closestPreviousPrice != null) {
                        // Aynı fiyatı kullan ama tarihi güncelle
                        resultMap[date] = PriceHistory(date, closestPreviousPrice.price)
                        println("DEBUG: Önceki tarihten veri kullanıldı: ${closestPreviousPrice.date} -> $date")
                    } else {
                        // Önceki tarih yoksa, sonraki en yakın tarihi bul
                        val closestNextPrice = priceHistory
                            .filter { priceDate -> 
                                val pDate = dateFormat.parse(priceDate.date)
                                pDate != null && pDate.after(dateObj)
                            }
                            .minByOrNull { it.date }
                        
                        if (closestNextPrice != null) {
                            resultMap[date] = PriceHistory(date, closestNextPrice.price)
                            println("DEBUG: Sonraki tarihten veri kullanıldı: ${closestNextPrice.date} -> $date")
                        }
                    }
                } catch (e: Exception) {
                    println("DEBUG: Tarih işleme hatası: ${e.message}")
                }
            }
        }
        
        return resultMap.values.toList().sortedBy { it.date }
    }
    
    // Tarih aralığındaki verileri filtrele
    val filtered = priceHistory.filter {
        try {
            val date = dateFormat.parse(it.date)
            val result = date != null && !date.before(startDate) && !date.after(today)
            println("DEBUG: Tarih: ${it.date}, Dahil mi: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Hata: ${e.message}")
            false
        }
    }
    
    // Aylık ve yıllık görünümler için veri noktası sayısını sınırla
    val result = when (timeRange) {
        TimeRange.MONTH -> {
            // Aylık görünüm için yaklaşık 30 veri noktası
            if (filtered.size > 30) {
                val step = filtered.size / 30
                filtered.filterIndexed { index, _ -> index % step == 0 }
            } else {
                filtered
            }
        }
        TimeRange.YEAR -> {
            // Yıllık görünüm için yaklaşık 12 veri noktası (ayda bir)
            if (filtered.size > 12) {
                val step = filtered.size / 12
                filtered.filterIndexed { index, _ -> index % step == 0 }
            } else {
                filtered
            }
        }
        else -> filtered
    }.sortedBy { it.date }
    
    println("DEBUG: Son filtreleme sonrası veri sayısı: ${result.size}")
    return result
}

// X ekseni için tarih formatı
private fun formatDateForXAxis(dateString: String, timeRange: TimeRange): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = try {
        inputFormat.parse(dateString)
    } catch (e: Exception) {
        println("DEBUG: Tarih ayrıştırma hatası: $dateString - ${e.message}")
        null
    } ?: return ""
    
    println("DEBUG: formatDateForXAxis - Tarih: $dateString")
    
    return when (timeRange) {
        TimeRange.WEEK -> {
            // Haftalık görünümde gün ve ay
            val calendar = Calendar.getInstance()
            calendar.time = date
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = SimpleDateFormat("MMM", Locale("tr")).format(date)
            val result = "$day $month"
            println("DEBUG: Haftalık format: $result")
            result
        }
        TimeRange.MONTH -> {
            // Aylık görünümde gün ve ay
            val calendar = Calendar.getInstance()
            calendar.time = date
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = SimpleDateFormat("MMM", Locale("tr")).format(date)
            "$day $month"
        }
        TimeRange.YEAR -> {
            // Yıllık görünümde ay ve yıl
            val calendar = Calendar.getInstance()
            calendar.time = date
            val month = SimpleDateFormat("MMM", Locale("tr")).format(date)
            val year = calendar.get(Calendar.YEAR)
            "$month $year"
        }
    }
}