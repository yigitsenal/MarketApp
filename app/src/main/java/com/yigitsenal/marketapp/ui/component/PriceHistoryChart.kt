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
                    TimeRange.MONTH -> xAxis.labelCount = minOf(dateLabels.size, 10) // Aylık görünümde 10 etiket
                    TimeRange.YEAR -> xAxis.labelCount = minOf(dateLabels.size, 12) // Yıllık görünümde 12 etiket
                }
                
                // Y ekseni ayarları
                axisLeft.setDrawGridLines(true)
                axisLeft.axisMinimum = 0f
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
                                if (entries.size > 10 && entries.indexOf(Entry(entries.indexOf(Entry(0f, value)).toFloat(), value)) % 2 != 0) {
                                    ""
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
    
    // Bugünün tarihini manuel olarak ayarla (17 Mart 2025)
    val currentCalendar = Calendar.getInstance()
    currentCalendar.set(2025, Calendar.MARCH, 17, 23, 59, 59)
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
            startCalendar.add(Calendar.DAY_OF_YEAR, -6) // Bugün dahil 7 gün
        }
        TimeRange.MONTH -> {
            // Son 30 günü göster (bugün dahil)
            startCalendar.add(Calendar.DAY_OF_YEAR, -29) // Bugün dahil 30 gün
        }
        TimeRange.YEAR -> {
            // Son 365 günü göster (bugün dahil)
            startCalendar.add(Calendar.DAY_OF_YEAR, -364) // Bugün dahil 365 gün
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
                resultMap[date] = pricesForDate.maxByOrNull { it.date } ?: pricesForDate.first()
                println("DEBUG: Tarih için veri bulundu: $date")
            } else {
                println("DEBUG: Tarih için veri bulunamadı: $date")
                // Eğer o gün için veri yoksa, en yakın önceki günün verisini bul
                val closestPreviousPrice = priceHistory
                    .filter { dateFormat.parse(it.date)?.before(dateFormat.parse(date)) ?: false }
                    .maxByOrNull { it.date }
                
                if (closestPreviousPrice != null) {
                    // Aynı fiyatı kullan ama tarihi güncelle
                    resultMap[date] = PriceHistory(date, closestPreviousPrice.price)
                    println("DEBUG: Önceki tarihten veri kullanıldı: ${closestPreviousPrice.date} -> $date")
                } else {
                    // Önceki tarih yoksa, sonraki en yakın tarihi bul
                    val closestNextPrice = priceHistory
                        .filter { dateFormat.parse(it.date)?.after(dateFormat.parse(date)) ?: false }
                        .minByOrNull { it.date }
                    
                    if (closestNextPrice != null) {
                        resultMap[date] = PriceHistory(date, closestNextPrice.price)
                        println("DEBUG: Sonraki tarihten veri kullanıldı: ${closestNextPrice.date} -> $date")
                    }
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
    return when (timeRange) {
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
}

// X ekseni için tarih formatı
private fun formatDateForXAxis(dateString: String, timeRange: TimeRange): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = inputFormat.parse(dateString) ?: return ""
    
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