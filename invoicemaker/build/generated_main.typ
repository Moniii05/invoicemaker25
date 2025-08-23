#let data = ( "issuing-date": "2025-07-26", recipient: ( name: "", address: ( city: "", street: "", "postal-code": "" ), "vat-id": "" ), "invoice-id": "Re0001", items: [ ( description: "frjg", total: 0.0, quantity: 1.0, price: 0.0, number: 1 ) ], biller: ( "vat-id": "DE123456789", address: ( city: "Musterstadt", street: "Musterstraße 1", "postal-code": "12345" ), name: "Mein Unternehmen GmbH", "bank-name": "Berliner Sparkasse", iban: "DE89370400440532013000" ), "delivery-date": "2025-07-26", language: "de", "due-date": "2025-08-09", "customer-id": "" )


#import "@preview/invoice-maker:1.1.0": *
#import "invoice-maker.typ": *

#show: invoice.with(data: data)
