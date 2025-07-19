#let nbh = "-"

//#let map = (seq, f) => [
//  for x in seq: f(x)
//]
//#let enumerate = seq => [
//  for i in 0..seq.len(): (i, seq.at(i))
//]

// Truncate a number to 2 decimal places
// and add trailing zeros if necessary
// E.g. 1.234 -> 1.23, 1.2 -> 1.20
#let add-zeros = (num) => {
  // Can't use trunc and fract due to rounding errors
  let frags = str(num).split(".")
  let (intp, decp) = if frags.len() == 2 { frags } else { (num, "00") }
  str(intp) + "," + (str(decp) + "00").slice(0, 2)
}

// From https://stackoverflow.com/a/57080936/1850340
#let verify-iban = (country, iban) => {
  let iban-regexes = (
    DE: regex("^DE[a-zA-Z0-9]{2}\\s?([0-9]{4}\\s?){4}([0-9]{2})$"),
    FR: regex("^FR[a-zA-Z0-9]{2}\\s?([0-9]{4}\\s?){5}([0-9]{3})$"),
    GB: regex("^GB[a-zA-Z0-9]{2}\\s?([a-zA-Z]{4}\\s?){1}([0-9]{4}\\s?){3}([0-9]{2})$")
  )

  if country == none or not country in iban-regexes {
    true
  } else {
    iban.find(iban-regexes.at(country)) != none
  }
}

#let parse-date = (date-str) => {
  let parts = date-str.split("-")
  if parts.len() != 3 {
    panic(
      "Invalid date string: " + date-str + "\n" +
      "Expected format: YYYY-MM-DD"
    )
  }
  datetime(
    year: int(parts.at(0)),
    month: int(parts.at(1)),
    day: int(parts.at(2))
  )
}

#let TODO = box(
  inset: (x: 0.5em),
  outset: (y: 0.2em),
  radius: 0.2em,
  fill: rgb(255,180,170)
)[
  #text(
    size: 0.8em,
    weight: 600,
    fill: rgb(100,68,64)
  )[TODO]
]

#let horizontalrule = [
  #v(8mm)
  #line(
    start: (20%, 0%),
    end: (80%, 0%),
    stroke: 0.8pt + gray
  )
  #v(8mm)
]

#let signature-line = line(length: 5cm, stroke: 0.4pt)

#let endnote(num, contents) = [
  #stack(dir: ltr, spacing: 3pt, super[#num], contents)
]

#let languages = (
  en: (
    id: "en",
    country: "GB",
    recipient: "Recipient",
    biller: "Biller",
    invoice: "Invoice",
    cancellation-invoice: "Cancellation Invoice",
    cancellation-notice: (id, issuing-date) => [
      As agreed, you will receive a credit note
      for the invoice *#id* dated *#issuing-date*.
    ],
    invoice-id: "Invoice ID",
    issuing-date: "Issuing Date",
    delivery-date: "Delivery Date",
    items: "Items",
    closing: "Thank you for the good cooperation!",
    number: "№",
    date: "Date",
    description: "Description",
    duration: "Duration",
    quantity: "Quantity",
    price: "Price",
    total-time: "Total working time",
    subtotal: "Subtotal",
    discount-of: "Discount of",
    vat: "VAT of",
    no-vat: "Not Subject to VAT",
    reverse-charge: "Reverse Charge",
    total: "Total",
    due-text: val => [Please transfer the money onto following bank account due to *#val* :],
    owner: "Owner",
    iban: "IBAN"
  ),
  fr: (
    id: "fr",
    country: "FR",
    recipient: "Destinataire",
    biller: "Émetteur",
    invoice: "Facture",
    cancellation-invoice: "Annulation de facture",
    cancellation-notice: (id, issuing-date) => [
      Comme convenu, vous recevrez un crédit
      pour la facture *#id* du *#issuing-date*.
    ],
    invoice-id: "Facture N°",
    issuing-date: "Date d’émission",
    delivery-date: "Date de livraison",
    items: "Produits",
    closing: "Merci !",
    number: "N°",
    date: "Date",
    description: "Description",
    duration: "Durée",
    quantity: "Quantité",
    price: "Prix",
    total-time: "Temps total travaillé",
    subtotal: "Sous-total",
    discount-of: "Remise de",
    vat: "TVA",
    no-vat: "Non sujet à la TVA",
    reverse-charge: "Facturation inversée",
    total: "Total",
    due-text: val => [Merci de régler d’ici le *#val* par virement au compte bancaire suivant :],
    owner: "Titulaire",
    iban: "IBAN"
  ),
  de: (
    id: "de",
    country: "DE",
    recipient: "Empfänger",
    biller: "Aussteller",
    invoice: "Rechnung",
    cancellation-invoice: "Stornorechnung",
    cancellation-notice: (id, issuing-date) => [
      Vereinbarungsgemäß erhalten Sie hiermit eine Gutschrift
      zur Rechnung *#id* vom *#issuing-date*.
    ],
    invoice-id: "Rechnungsnummer",
    issuing-date: "Ausstellungsdatum",
    delivery-date: "Lieferdatum",
    customer-id: "Kundennummer",
    items: "Leistungen",
    closing: "Vielen Dank für die gute Zusammenarbeit!",
    number: "Nr",
    date: "Datum",
    description: "Beschreibung",
    quantity: "Menge",
    price: "Preis",
    total-time: "Gesamtarbeitszeit",
    subtotal: "Zwischensumme",
    discount-of: "Rabatt von",
    vat: "Umsatzsteuer von",
    no-vat: "Nicht umsatzsteuerpflichtig",
    reverse-charge: "Steuerschuldnerschaft des\nLeistungsempfängers",
    total: "Gesamt",
    due-text: val => [Bitte überweisen Sie den Betrag bis zum *#val* auf folgendes Konto :],
    owner: "Inhaber",
    iban: "IBAN",
    bank-name: "Bank"
  )
)

#let invoice(
  language: "en",
  currency: "€",
  country: none,
  title: none,
  banner-image: none,
  invoice-id: none,
  cancellation-id: none,
  issuing-date: none,
  delivery-date: none,
  due-date: none,
  customer-id: none,
  biller: (:),
  recipient: (:),
  keywords: (),
  hourly-rate: none,
  styling: (:),
  items: (),
  discount: none,
  vat: 0.19,
  data: none,
  override-translation: none,
  doc
) = {
  styling.font = styling.at("font", default: "Liberation Sans")
  styling.font-size = styling.at("font-size", default: 11pt)
  styling.margin = styling.at("margin", default: (
    top: 20mm,
    right: 25mm,
    bottom: 20mm,
    left: 25mm
  ))

  language = if data != none { data.at("language", default: language) } else { language }

  let t = if type(language) == str { languages.at(language) }
          else if type(language) == dictionary { language }
          else { panic("Language must be a string or dictionary.") }

  if override-translation != none {
    for k in t.keys() {
      if override-translation.at(k, default: none) != none {
        t.insert(k, override-translation.at(k))
      }
    }
  }

  if data != none {
    language = data.at("language", default: language)
    currency = data.at("currency", default: currency)
    country = data.at("country", default: t.country)
    title = data.at("title", default: title)
    banner-image = data.at("banner-image", default: banner-image)
    invoice-id = data.at("invoice-id", default: invoice-id)
    cancellation-id = data.at("cancellation-id", default: cancellation-id)
    issuing-date = data.at("issuing-date", default: issuing-date)
    delivery-date = data.at("delivery-date", default: delivery-date)
    due-date = data.at("due-date", default: due-date)
    customer-id = data.at("customer-id", default: customer-id)
    biller = data.at("biller", default: biller)
    recipient = data.at("recipient", default: recipient)
    keywords = data.at("keywords", default: keywords)
    hourly-rate = data.at("hourly-rate", default: hourly-rate)
    styling = data.at("styling", default: styling)
    items = data.at("items", default: items)
    discount = data.at("discount", default: discount)
    vat = data.at("vat", default: vat)
  }

  assert(
    verify-iban(country, biller.iban),
    message: "Invalid IBAN " + biller.iban + " for country " + country
  )

  let issuing-date = if issuing-date != none {
    if type(issuing-date) == str { parse-date(issuing-date) } else { issuing-date }
  } else {
    datetime.today()
  }

  let delivery-date = if delivery-date != none {
    if type(delivery-date) == str { parse-date(delivery-date) } else { delivery-date }
  } else {
    none
  }

  set document(title: title, keywords: keywords, date: issuing-date)
  set page(margin: styling.margin, numbering: none)
  set par(justify: true)
  set text(lang: t.id, font: styling.font, size: styling.font-size)
  set table(stroke: none)

  [#pad(top: -20mm, banner-image)]

  align(center)[
    #block(inset: 2em)[
      #text(weight: "bold", size: 2em)[
        if title != none { title }
        else if cancellation-id != none { t.cancellation-invoice }
        else { t.invoice }
      ]
    ]
  ]

  let invoice-id-norm = if invoice-id != none {
    if cancellation-id != none { cancellation-id } else { invoice-id }
  } else {
    TODO
  }

  align(center,
    table(
      columns: (auto, auto),
      align: (top, top),
      inset: 4pt,
      [#t.invoice-id:],     [*#invoice-id-norm*],
      [#t.issuing-date:],   [issuing-date.display("[day].[month].[year]")],
      [#t.delivery-date:],  [
        if delivery-date != none {
          delivery-date.display("[day].[month].[year]")
        } else {
          ""
        }
      ],
      [#t.customer-id:],    [#customer-id]
    )
  )

  v(2em)

  box(height: 10em)[
    #columns(2, gutter: 4em)[
      === #t.recipient
      #recipient.name \
      #{if "title" in recipient { [#recipient.title] }}
      #{if "country" in recipient.address { [#recipient.address.country] }}
      #recipient.address.street\
      #recipient.address.city #recipient.address.postal-code

      === #t.biller
      #biller.name \
      #{if "title" in biller { [#biller.title] }}
      #{if "country" in biller.address { [#biller.address.country] }}
      #biller.address.street \
      #biller.address.city #biller.address.postal-code \
      #{if biller.vat-id != none { "USt-IdNr.: " + biller.vat-id } else { "" }}
    ]
  ]

  if cancellation-id != none {
    (t.cancellation-notice)(invoice-id, issuing-date)
  }

  [== #t.items]

  v(1em)

  let getRowTotal = row => {
    if row.at("dur-min", default: 0) == 0 {
      row.price * row.at("quantity", default: 1)
    } else {
      calc.round(hourly-rate * (row.dur-min / 60), digits: 2)
    }
  }

  let cancel-neg = if cancellation-id != none { -1 } else { 1 }


table(
  columns: (auto, 2fr, auto, auto, auto),
  inset: 6pt,

  // Kopfzeile
  [#t.number:], [#t.description:], …,

  // *Hier* darf die Schleife stehen:
  for (i, row) in enumerate(items):
    (
      i + 1,
      row.description,
      str(row.quantity),
      str(add-zeros(row.price)),
      str(add-zeros(getRowTotal(row)))
    ),

  table.hline(stroke: 0.5pt),
)


   
 

  let sub-total = items.map(getRowTotal).sum()
  let total-duration = items.map(row => int(row.at("dur-min", default: 0))).sum()

  let discount-value = if discount == none { 0 }
    else if discount.type == "fixed" { discount.value }
    else if discount.type == "proportionate" { sub-total * discount.value }
    else { panic("Invalid discount type") }

  let discount-label = if discount == none { "" }
    else if discount.type == "fixed" { str(discount.value) + " " + currency }
    else if discount.type == "proportionate" { str(discount.value * 100) + " %" }
    else { "" }

  let has-reverse-charge = biller.vat-id.slice(0,2) != recipient.vat-id.slice(0,2)
  let tax = if has-reverse-charge { 0 } else { sub-total * vat }
  let total = sub-total - discount-value + tax

  let table-entries = (
    ([#t.subtotal:], [#add-zeros(cancel-neg * sub-total) #currency]),
    if discount-value != 0 {
      ([#t.discount-of #discount-label], [#add-zeros(-cancel-neg * discount-value) #currency])
    },
    if not has-reverse-charge and vat != 0 {
      ([#t.vat #{vat*100} %:], [#add-zeros(cancel-neg * tax) #currency])
    },
    if vat == 0 { ([#t.no-vat], [""]) },
    if has-reverse-charge { ([#t.vat:], text(0.9em)[#t.reverse-charge]) },
    ([*#t.total*:], [*#add-zeros(cancel-neg * total) #currency*])
  ).filter(entry => entry != none)

  let grayish = luma(245)

align(right)[
  table(
    columns: 2,
    inset: (col, row) => (…), 

    // eine Zeile pro Eintrag
    for entry in table-entries:
      (entry.at(0), entry.at(1)),

    table.hline(stroke: 0.5pt),
  )
]

  
 

  v(2em)

  if cancellation-id == none {
    let due-date = if due-date != none { due-date } else { issuing-date + duration(days:14) }
    (t.due-text)(due-date.display("[day].[month].[year]"))
    v(1em)
    align(center)[
      table(
        fill: grayish,
        columns: (8em, auto),
        inset: (col,row) => (
          top:0.5em, bottom:0.8em,
          left: if col==0 {0.6efm} else {1em},
          right: if col==0 {1em} else {0.6em}
        ),
        align: (right,left),
        [#t.owner:], [*#biller.name*],
        [#t.bank-name:], [*#biller.bank-name*],
        [#t.iban:], [*#biller.iban*],
        table.hline(stroke: 0.5pt),
      )
    ]
    v(1em)
    t.closing
  } else {
    v(1em)
    align(center, strong(t.closing))
  }

  doc
}



