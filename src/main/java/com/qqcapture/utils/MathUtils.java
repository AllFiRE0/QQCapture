package com.qqcapture.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class MathUtils {
    private static final Random RANDOM = new Random();
    
    /**
     * Ограничивает значение между минимумом и максимумом
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Вычисляет процент от числа
     */
    public static int percentage(int total, int part) {
        if (total == 0) {
            return 0;
        }
        return (int) ((double) part / total * 100);
    }
    
    public static double percentage(double total, double part) {
        if (total == 0) {
            return 0;
        }
        return (part / total) * 100;
    }
    
    public static float percentage(float total, float part) {
        if (total == 0) {
            return 0;
        }
        return (part / total) * 100;
    }
    
    /**
     * Округляет double до int
     */
    public static int roundToInt(double value) {
        return (int) Math.round(value);
    }
    
    public static long roundToLong(double value) {
        return Math.round(value);
    }
    
    /**
     * Округляет с заданной точностью
     */
    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException("places must be >= 0");
        }
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    
    /**
     * Проверка на четность/нечетность
     */
    public static boolean isEven(int number) {
        return number % 2 == 0;
    }
    
    public static boolean isOdd(int number) {
        return number % 2 != 0;
    }
    
    /**
     * Генерация случайных чисел
     */
    public static int getRandomInt(int min, int max) {
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }
        return RANDOM.nextInt((max - min) + 1) + min;
    }
    
    public static double getRandomDouble(double min, double max) {
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }
        return min + (max - min) * RANDOM.nextDouble();
    }
    
    public static float getRandomFloat(float min, float max) {
        if (min > max) {
            float temp = min;
            min = max;
            max = temp;
        }
        return min + (max - min) * RANDOM.nextFloat();
    }
    
    public static boolean getRandomBoolean() {
        return RANDOM.nextBoolean();
    }
    
    public static boolean getRandomChance(int percent) {
        if (percent <= 0) return false;
        if (percent >= 100) return true;
        return RANDOM.nextInt(100) < percent;
    }
    
    public static boolean getRandomChance(double percent) {
        if (percent <= 0) return false;
        if (percent >= 100) return true;
        return RANDOM.nextDouble() * 100 < percent;
    }
    
    /**
     * Сложение с защитой от переполнения
     */
    public static int safeAdd(int a, int b) {
        long result = (long) a + (long) b;
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) result;
    }
    
    public static long safeAdd(long a, long b) {
        long result = a + b;
        if (result < a || result < b) {
            // Переполнение
            return Long.MAX_VALUE;
        }
        return result;
    }
    
    public static double safeAdd(double a, double b) {
        double result = a + b;
        if (Double.isInfinite(result) || Double.isNaN(result)) {
            return Double.MAX_VALUE;
        }
        return result;
    }
    
    /**
     * Умножение с защитой от переполнения
     */
    public static int safeMultiply(int a, int b) {
        long result = (long) a * (long) b;
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) result;
    }
    
    public static long safeMultiply(long a, long b) {
        long result = a * b;
        if (a != 0 && result / a != b) {
            return Long.MAX_VALUE;
        }
        return result;
    }
    
    /**
     * Вычисление среднего значения
     */
    public static double average(double... values) {
        if (values.length == 0) {
            return 0;
        }
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }
    
    public static int average(int... values) {
        if (values.length == 0) {
            return 0;
        }
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return sum / values.length;
    }
    
    /**
     * Сравнение double с точностью
     */
    public static boolean equals(double a, double b, double epsilon) {
        return Math.abs(a - b) < epsilon;
    }
    
    public static boolean equals(double a, double b) {
        return equals(a, b, 0.0000001);
    }
    
    /**
     * Проверка на степень двойки
     */
    public static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    /**
     * Возведение в степень
     */
    public static int pow(int base, int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("exponent must be >= 0");
        }
        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result = safeMultiply(result, base);
        }
        return result;
    }
    
    /**
     * Факториал
     */
    public static long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be >= 0");
        }
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
    
    /**
     * НОД (наибольший общий делитель)
     */
    public static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
    
    /**
     * НОК (наименьшее общее кратное)
     */
    public static int lcm(int a, int b) {
        return a / gcd(a, b) * b;
    }
    
    /**
     * Сигнум (знак числа)
     */
    public static int signum(int x) {
        return Integer.compare(x, 0);
    }
    
    public static int signum(double x) {
        return Double.compare(x, 0.0);
    }
    
    /**
     * Линейная интерполяция
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
    
    /**
     * Обратная линейная интерполяция
     */
    public static double inverseLerp(double a, double b, double value) {
        return (value - a) / (b - a);
    }
    
    /**
     * Нормализация значения в диапазон [0, 1]
     */
    public static double normalize(double value, double min, double max) {
        return (value - min) / (max - min);
    }
    
    /**
     * Преобразование тиков в секунды
     */
    public static int ticksToSeconds(int ticks) {
        return ticks / 20;
    }
    
    public static int secondsToTicks(int seconds) {
        return seconds * 20;
    }
    
    /**
     * Форматирование чисел
     */
    public static String formatNumber(int number) {
        return String.format("%,d", number);
    }
    
    public static String formatNumber(double number) {
        return String.format("%,.2f", number);
    }
    
    public static String formatNumber(double number, int decimals) {
        return String.format("%,." + decimals + "f", number);
    }
}
