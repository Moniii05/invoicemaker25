#let data = ( biller: ( iban: "DE89370400440532013000", "vat-id": "DE123456789", address: ( "postal-code": "12345", city: "Musterstadt", street: "Musterstraße 1" ), name: "Mein Unternehmen GmbH", "bank-name": "Berliner Sparkasse" ), "delivery-date": "2025-07-08", language: "de", "due-date": "2025-07-22", "customer-id": "555", "issuing-date": "2025-07-08", recipient: ( "vat-id": "", name: "tini", address: ( "postal-code": "123445", city: "Berlin", street: "blumen str . 200" ) ), "invoice-id": "Re0001", items: [ ( quantity: 1.0, price: 0.0, number: 1, description: "viiii", total: 0.0 ) ] )


#import "@preview/invoice-maker:1.1.0": *
#import "invoice-maker.typ": *

#show: invoice.with(data: data)
