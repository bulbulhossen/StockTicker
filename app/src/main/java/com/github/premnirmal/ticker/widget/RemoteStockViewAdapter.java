package com.github.premnirmal.ticker.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.WidgetClickReceiver;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.tickerwidget.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by premnirmal on 12/21/14.
 */
public class RemoteStockViewAdapter implements RemoteViewsService.RemoteViewsFactory {

    private List<Stock> stocks;
    private Context context;

    @Inject
    IStocksProvider stocksProvider;

    public RemoteStockViewAdapter(Context context) {
        Injector.inject(this);
        Collection<Stock> stocks = stocksProvider.getStocks();
        this.stocks = stocks == null ? new ArrayList<Stock>() : new ArrayList<>(stocks);
        this.context = context;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        final Collection<Stock> stocks = stocksProvider.getStocks();
        this.stocks = stocks == null ? new ArrayList<Stock>() : new ArrayList<Stock>(stocks);
    }

    @Override
    public void onDestroy() {
        context = null;
    }

    @Override
    public int getCount() {
        return stocks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        final int stockViewLayout = Tools.stockViewLayout();
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), stockViewLayout);
        final Stock stock = stocks.get(position);
        remoteViews.setTextViewText(R.id.ticker, stock.symbol);

        final double change;
        final SpannableString changePercentString, changeValueString, priceString;
        {
            final double changePercent;
            if (stock.Change != null) {
                change = Double.parseDouble(stock.Change.replace("+", ""));
                changePercent = Double.parseDouble(stock.ChangeinPercent.replace("+", "").replace("%", ""));
            } else {
                change = 0d;
                changePercent = 0d;
            }
            final String changeValueFormatted = Tools.DECIMAL_FORMAT.format(change);
            final String changePercentFormatted = Tools.DECIMAL_FORMAT.format(changePercent);
            final String priceFormatted = Tools.DECIMAL_FORMAT.format(stock.LastTradePriceOnly);

            changePercentString = new SpannableString(changePercentFormatted + "%");
            changeValueString = new SpannableString(changeValueFormatted);
            priceString = new SpannableString(priceFormatted);

            if (Tools.boldEnabled() && stock.ChangeinPercent != null && stock.Change != null) {
                changePercentString.setSpan(new StyleSpan(Typeface.BOLD), 0, changePercentString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                changeValueString.setSpan(new StyleSpan(Typeface.BOLD), 0, changeValueString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        if (stockViewLayout == R.layout.stockview3) {
            final Tools.ChangeType changeType = Tools.getChangeType();
            if (changeType == Tools.ChangeType.percent) {
                remoteViews.setTextViewText(R.id.change, changePercentString);
            } else {
                remoteViews.setTextViewText(R.id.change, changeValueString);
            }
        } else {
            remoteViews.setTextViewText(R.id.changePercent, changePercentString);
            remoteViews.setTextViewText(R.id.changeValue, changeValueString);
        }
        remoteViews.setTextViewText(R.id.totalValue, priceString);


        final int color;
        if (change >= 0) {
            color = context.getResources().getColor(R.color.positive_green);
        } else {
            color = context.getResources().getColor(R.color.negative_red);
        }
        if (stockViewLayout == R.layout.stockview3) {
            remoteViews.setTextColor(R.id.change, color);
        } else {
            remoteViews.setTextColor(R.id.changePercent, color);
            remoteViews.setTextColor(R.id.changeValue, color);
        }

        remoteViews.setTextColor(R.id.ticker, Tools.getTextColor(context));
        remoteViews.setTextColor(R.id.totalValue, Tools.getTextColor(context));

        final float fontSize = Tools.getFontSize(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (stockViewLayout == R.layout.stockview3) {
                remoteViews.setTextViewTextSize(R.id.change, TypedValue.COMPLEX_UNIT_SP, fontSize);
            } else {
                remoteViews.setTextViewTextSize(R.id.changePercent, TypedValue.COMPLEX_UNIT_SP, fontSize);
                remoteViews.setTextViewTextSize(R.id.changeValue, TypedValue.COMPLEX_UNIT_SP, fontSize);
            }
            remoteViews.setTextViewTextSize(R.id.ticker, TypedValue.COMPLEX_UNIT_SP, fontSize);
            remoteViews.setTextViewTextSize(R.id.totalValue, TypedValue.COMPLEX_UNIT_SP, fontSize);
        }

        if(stockViewLayout == R.layout.stockview3) {
            final Intent intent = new Intent();
            intent.putExtra(WidgetClickReceiver.FLIP, true);
            remoteViews.setOnClickFillInIntent(R.id.change, intent);
        }
        remoteViews.setOnClickFillInIntent(R.id.ticker, new Intent());

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return new RemoteViews(context.getPackageName(), R.layout.loadview);
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
