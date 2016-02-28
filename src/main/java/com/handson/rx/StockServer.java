package com.handson.rx;

import com.handson.dto.Quote;
import com.handson.infra.EventStreamClient;
import com.handson.infra.RxNettyEventServer;
import rx.Observable;
import rx.subjects.ReplaySubject;

import java.util.List;
import java.util.Map;


public class StockServer extends RxNettyEventServer<Quote> {

    private final EventStreamClient stockEventStreamClient;
    private final EventStreamClient forexEventStreamClient;

    public StockServer(int port, EventStreamClient stockEventStreamClient, EventStreamClient forexEventStreamClient) {
        super(port);
        this.stockEventStreamClient = stockEventStreamClient;
        this.forexEventStreamClient = forexEventStreamClient;
    }

    @Override
    protected Observable<Quote> getEvents(Map<String, List<String>> parameters) {

        /* Etape initiale
        String stockCode = parameters.get("STOCK").get(0);
        return stockClient
            .readServerSideEvents()
            .map(Quote::fromJson);
        */

        /* Etape 1 : filtre sur code de la stock
        String stockCode = parameters.get("STOCK").get(0);

        return stockEventStreamClient
                .readServerSideEvents()
                .map(Quote::fromJson)
                .filter(q -> q.code.equals(stockCode));
        */


        /* Etape 2 : application du forex
        String stockCode = parameters.get("STOCK").get(0);

        Observable<Quote> quotes = stockEventStreamClient
                .readServerSideEvents()
                .map(Quote::fromJson)
                .filter(q -> q.code.equals(stockCode))
                .doOnNext(System.out::println);

        Observable<Quote> eurUsd = forexEventStreamClient
                .readServerSideEvents()
                .map(Quote::fromJson)
                .doOnNext(System.out::println);

        return quotes.flatMap(q ->
                eurUsd.take(1).map(fx -> new Quote(q.code, q.quote/fx.quote))
        );
        */

        /* Etape 3 : application forex avec mise en cache */
        String stockCode = parameters.get("STOCK").get(0);

        Observable<Quote> quotes = stockEventStreamClient
                .readServerSideEvents()
                .map(Quote::fromJson)
                .filter(q -> q.code.equals(stockCode))
                .doOnNext(System.out::println);

        Observable<Quote> eurUsd = forexEventStreamClient
                .readServerSideEvents()
                .map(Quote::fromJson)
                .doOnNext(System.out::println);

        ReplaySubject<Quote> eurUsdCached = ReplaySubject.create();
        eurUsd.subscribe(eurUsdCached);

        return quotes.flatMap(q ->
            eurUsdCached.take(1).map(fx -> new Quote(q.code, q.quote/fx.quote))
        );
    }
}