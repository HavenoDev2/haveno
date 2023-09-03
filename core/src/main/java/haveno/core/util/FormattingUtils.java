package haveno.core.util;

import haveno.common.util.MathUtils;
import haveno.core.locale.CurrencyUtil;
import haveno.core.locale.GlobalSettings;
import haveno.core.locale.Res;
import haveno.core.monetary.CryptoMoney;
import haveno.core.monetary.Price;
import haveno.core.monetary.TraditionalMoney;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Monetary;
import org.bitcoinj.utils.MonetaryFormat;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FormattingUtils {
    public static final String BTC_FORMATTER_KEY = "BTC";

    public final static String RANGE_SEPARATOR = " - ";

    private static final MonetaryFormat priceFormat4Decimals = new MonetaryFormat().shift(0).minDecimals(4).repeatOptionalDecimals(0, 0);
    private static final MonetaryFormat priceFormat8Decimals = new MonetaryFormat().shift(0).minDecimals(8).repeatOptionalDecimals(0, 0);
    private static final MonetaryFormat cryptoFormat = new MonetaryFormat().shift(0).minDecimals(CryptoMoney.SMALLEST_UNIT_EXPONENT).repeatOptionalDecimals(0, 0);
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");

    public static String formatCoinWithCode(long value, MonetaryFormat coinFormat) {
        return formatCoinWithCode(Coin.valueOf(value), coinFormat);
    }

    public static String formatCoinWithCode(Coin coin, MonetaryFormat coinFormat) {
        if (coin != null) {
            try {
                // we don't use the code feature from coinFormat as it does automatic switching between mBTC and BTC and
                // pre and post fixing
                return coinFormat.postfixCode().format(coin).toString();
            } catch (Throwable t) {
                log.warn("Exception at formatCoinWithCode: " + t.toString());
                return "";
            }
        } else {
            return "";
        }
    }

    public static String formatCoin(long value, MonetaryFormat coinFormat) {
        return formatCoin(Coin.valueOf(value), -1, false, 0, coinFormat);
    }

    public static String formatCoin(Coin coin,
                                    int decimalPlaces,
                                    boolean decimalAligned,
                                    int maxNumberOfDigits,
                                    MonetaryFormat coinFormat) {
        String formattedCoin = "";

        if (coin != null) {
            try {
                if (decimalPlaces < 0 || decimalPlaces > 4) {
                    formattedCoin = coinFormat.noCode().format(coin).toString();
                } else {
                    formattedCoin = coinFormat.noCode().minDecimals(decimalPlaces).repeatOptionalDecimals(1, decimalPlaces).format(coin).toString();
                }
            } catch (Throwable t) {
                log.warn("Exception at formatBtc: " + t.toString());
            }
        }

        if (decimalAligned) {
            formattedCoin = fillUpPlacesWithEmptyStrings(formattedCoin, maxNumberOfDigits);
        }

        return formattedCoin;
    }

    public static String formatTraditionalMoney(TraditionalMoney traditionalMoney, MonetaryFormat format, boolean appendCurrencyCode) {
        if (traditionalMoney != null) {
            try {
                final String res = format.noCode().format(traditionalMoney).toString();
                if (appendCurrencyCode)
                    return res + " " + traditionalMoney.getCurrencyCode();
                else
                    return res;
            } catch (Throwable t) {
                log.warn("Exception at formatTraditionalMoneyWithCode: " + t.toString());
                return Res.get("shared.na") + " " + traditionalMoney.getCurrencyCode();
            }
        } else {
            return Res.get("shared.na");
        }
    }

    private static String formatCrypto(CryptoMoney crypto, boolean appendCurrencyCode) {
        if (crypto != null) {
            try {
                String res = cryptoFormat.noCode().format(crypto).toString();
                if (appendCurrencyCode)
                    return res + " " + crypto.getCurrencyCode();
                else
                    return res;
            } catch (Throwable t) {
                log.warn("Exception at formatCrypto: " + t.toString());
                return Res.get("shared.na") + " " + crypto.getCurrencyCode();
            }
        } else {
            return Res.get("shared.na");
        }
    }

    public static String formatCryptoVolume(CryptoMoney crypto, boolean appendCurrencyCode) {
        if (crypto != null) {
            try {
                // TODO quick hack...
                String res;
                if (crypto.getCurrencyCode().equals("BSQ"))
                    res = cryptoFormat.noCode().minDecimals(2).repeatOptionalDecimals(0, 0).format(crypto).toString();
                else
                    res = cryptoFormat.noCode().format(crypto).toString();
                if (appendCurrencyCode)
                    return res + " " + crypto.getCurrencyCode();
                else
                    return res;
            } catch (Throwable t) {
                log.warn("Exception at formatCryptoVolume: " + t.toString());
                return Res.get("shared.na") + " " + crypto.getCurrencyCode();
            }
        } else {
            return Res.get("shared.na");
        }
    }

    public static String formatPrice(Price price, MonetaryFormat priceFormat, boolean appendCurrencyCode) {
        if (price != null) {
            Monetary monetary = price.getMonetary();
            if (monetary instanceof TraditionalMoney)
                return formatTraditionalMoney((TraditionalMoney) monetary, priceFormat, appendCurrencyCode);
            else
                return formatCrypto((CryptoMoney) monetary, appendCurrencyCode);
        } else {
            return Res.get("shared.na");
        }
    }

    public static String formatPrice(Price price, boolean appendCurrencyCode) {
        return formatPrice(price, getPriceMonetaryFormat(price.getCurrencyCode()), appendCurrencyCode);
    }

    public static String formatPrice(Price price) {
        return formatPrice(price, price == null ? null : getPriceMonetaryFormat(price.getCurrencyCode()), false);
    }

    public static String formatMarketPrice(double price, String currencyCode) {
        if (CurrencyUtil.isTraditionalCurrency(currencyCode))
            return formatMarketPrice(price, 2);
        else
            return formatMarketPrice(price, 8);
    }

    public static String formatMarketPrice(double price, int precision) {
        return formatRoundedDoubleWithPrecision(price, precision);
    }

    public static String formatRoundedDoubleWithPrecision(double value, int precision) {
        decimalFormat.setMinimumFractionDigits(precision);
        decimalFormat.setMaximumFractionDigits(precision);
        return decimalFormat.format(MathUtils.roundDouble(value, precision)).replace(",", ".");
    }

    public static String formatDateTime(Date date, boolean useLocaleAndLocalTimezone) {
        Locale locale = useLocaleAndLocalTimezone ? GlobalSettings.getLocale() : Locale.US;
        DateFormat dateInstance = DateFormat.getDateInstance(DateFormat.DEFAULT, locale);
        DateFormat timeInstance = DateFormat.getTimeInstance(DateFormat.DEFAULT, locale);
        if (!useLocaleAndLocalTimezone) {
            dateInstance.setTimeZone(TimeZone.getTimeZone("UTC"));
            timeInstance.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        return formatDateTime(date, dateInstance, timeInstance);
    }

    public static String formatDateTime(Date date, DateFormat dateFormatter, DateFormat timeFormatter) {
        if (date != null) {
            return dateFormatter.format(date) + " " + timeFormatter.format(date);
        } else {
            return "";
        }
    }

    public static String getDateFromBlockHeight(long blockHeight) {
        long now = new Date().getTime();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM", Locale.getDefault());
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return formatDateTime(new Date(now + blockHeight * 10 * 60 * 1000L), dateFormatter, timeFormatter);
    }

    public static String formatToPercentWithSymbol(double value) {
        return formatToPercent(value) + "%";
    }

    public static String formatToRoundedPercentWithSymbol(double value) {
        return formatToPercent(value, new DecimalFormat("#")) + "%";
    }

    public static String formatPercentagePrice(double value) {
        return formatToPercentWithSymbol(value);
    }

    public static String formatToPercent(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setMinimumFractionDigits(2);
        decimalFormat.setMaximumFractionDigits(2);

        return formatToPercent(value, decimalFormat);
    }

    public static String formatToPercent(double value, DecimalFormat decimalFormat) {
        return decimalFormat.format(MathUtils.roundDouble(value * 100.0, 2)).replace(",", ".");
    }

    public static String formatDurationAsWords(long durationMillis) {
        return formatDurationAsWords(durationMillis, false, true);
    }

    public static String formatDurationAsWords(long durationMillis, boolean showSeconds, boolean showZeroValues) {
        String format = "";
        String second = Res.get("time.second");
        String minute = Res.get("time.minute");
        String hour = Res.get("time.hour").toLowerCase();
        String day = Res.get("time.day").toLowerCase();
        String days = Res.get("time.days");
        String hours = Res.get("time.hours");
        String minutes = Res.get("time.minutes");
        String seconds = Res.get("time.seconds");

        if (durationMillis >= TimeUnit.DAYS.toMillis(1)) {
            format = "d\' " + days + ", \'";
        }

        if (showSeconds) {
            format += "H\' " + hours + ", \'m\' " + minutes + ", \'s\' " + seconds + "\'";
        } else {
            format += "H\' " + hours + ", \'m\' " + minutes + "\'";
        }

        String duration = durationMillis > 0 ? DurationFormatUtils.formatDuration(durationMillis, format) : "";

        duration = StringUtils.replacePattern(duration, "^1 " + seconds + "|\\b1 " + seconds, "1 " + second);
        duration = StringUtils.replacePattern(duration, "^1 " + minutes + "|\\b1 " + minutes, "1 " + minute);
        duration = StringUtils.replacePattern(duration, "^1 " + hours + "|\\b1 " + hours, "1 " + hour);
        duration = StringUtils.replacePattern(duration, "^1 " + days + "|\\b1 " + days, "1 " + day);

        if (!showZeroValues) {
            duration = duration.replace(", 0 seconds", "");
            duration = duration.replace(", 0 minutes", "");
            duration = duration.replace(", 0 hours", "");
            duration = StringUtils.replacePattern(duration, "^0 days, ", "");
            duration = StringUtils.replacePattern(duration, "^0 hours, ", "");
            duration = StringUtils.replacePattern(duration, "^0 minutes, ", "");
            duration = StringUtils.replacePattern(duration, "^0 seconds, ", "");
        }
        return duration.trim();
    }

    public static String formatBytes(long bytes) {
        double kb = 1024;
        double mb = kb * kb;
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        if (bytes < kb)
            return bytes + " bytes";
        else if (bytes < mb)
            return decimalFormat.format(bytes / kb) + " KB";
        else
            return decimalFormat.format(bytes / mb) + " MB";
    }

    @NotNull
    public static String fillUpPlacesWithEmptyStrings(String formattedNumber, @SuppressWarnings("unused") int maxNumberOfDigits) {
        //FIXME: temporary deactivate adding spaces in front of numbers as we don't use a monospace font right now.
        /*int numberOfPlacesToFill = maxNumberOfDigits - formattedNumber.length();
        for (int i = 0; i < numberOfPlacesToFill; i++) {
            formattedNumber = " " + formattedNumber;
        }*/
        return formattedNumber;
    }

    public static MonetaryFormat getPriceMonetaryFormat(String currencyCode) {
        return CurrencyUtil.isPricePrecise(currencyCode) ? priceFormat8Decimals : priceFormat4Decimals;
    }
}
