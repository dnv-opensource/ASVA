(ns asvs.i18n
  (:require
   [clojure.string :as str]
   [tongue.core :as tongue]))

(def ^:private format-number-en
  (tongue/number-formatter {:group "," :decimal "."}))

(def ^:private inst-strings-en
  {:weekdays-narrow ["S" "M" "T" "W" "T" "F" "S"]
   :weekdays-short  ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"]
   :weekdays-long   ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"]
   :months-narrow   ["J" "F" "M" "A" "M" "J" "J" "A" "S" "O" "N" "D"]
   :months-short    ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]
   :months-long     ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"]
   :dayperiods      ["AM" "PM"]
   :eras-short      ["BC" "AD"]
   :eras-long       ["Before Christ" "Anno Domini"]})

(def ^:private is-finite? js/isFinite)

(defn ^:private round-to-decimals
  [n decimals]
  (if (is-finite? n)
    (let [factor (Math/pow 10 decimals)]
      (/ (Math/round (* n factor)) factor))
    0))

(def dicts
  {:en
   {:tongue/format-number format-number-en
    :tongue/format-inst (tongue/inst-formatter "{month-long} {day}, {year}" inst-strings-en)
    :date (tongue/inst-formatter "{month-long} {day}, {year}" inst-strings-en)
    :date-time (tongue/inst-formatter "{month-short} {day}, {year} {hour24}:{minutes-padded}" inst-strings-en)
    :12-hour (tongue/inst-formatter "{hour12}" inst-strings-en)
    :two-decimals (fn [num] (format-number-en (round-to-decimals num 2)))

    :main-title  "Welcome to Our ASVA Assessment Page"
    :preface-1 "Our mission is to integrate robust security practices into our simulation technology services. This assessment, aligned with "
    :preface-link-1 "OWASP's ASVA guidelines"
    :preface-2 ", ensures our applications are secure and resilient. We leverage resources like the "
    :preface-link-2 "OWASP Top Ten Proactive Controls"
    :preface-3 " to guide our approach, fostering a culture of security and awareness. Your expertise and collaboration in this process are key to maintaining our commitment to security excellence."

    :search "Search..."
    :copy-location "Copy location of anchor to your clipboard"
    :level "Level {1}"
    :not-applicable "Not applicable"
    :completed "Completed"

    :architecture "Architecture"
    :authentication "Authentication"
    :session-management "Session management"
    :access-control "Access control"
    :validation-sanitization-encoding "Validation Sanitization Encoding"
    :cryptography "Cryptography"
    :error-logging "Error logging"
    :data-protection "Data protection"
    :communications "Communications"
    :malicious "Malicious"
    :business-logic "Business logic"
    :files-resources "Files & resources"
    :api "API"
    :config "Config"}

   :nb
   {:main-title "Velkommen til vår ASVA-vurderingsside"
    :preface-1 "Vår misjon er å integrere robuste sikkerhetspraksiser i våre simuleringsteknologitjenester. Denne vurderingen, i tråd med "
    :preface-link-1 "OWASPs ASVA-retningslinjer"
    :preface-2 ", sikrer at våre applikasjoner er sikre og motstandsdyktige. Vi benytter ressurser som "
    :preface-link-2 "OWASPs topp ti proaktive kontroller"
    :preface-3 " for å lede vår tilnærming, og fremmer en kultur for sikkerhet og bevissthet. Din ekspertise og samarbeid i denne prosessen er nøkkelen til å opprettholde vårt engasjement for sikkerhetseksellens."

    :search "Søk..."
    :copy-location "Kopier plassering av anker til utklippstavlen din"
    :level "Nivå {1}"
    :not-applicable "Ikke relevant"
    :completed "Fullført"

    :architecture "Arkitektur"
    :authentication "Autentisering"
    :session-management "Sesjonsstyring"
    :access-control "Tilgangskontroll"
    :validation-sanitization-encoding "Validering, Sanitering, Koding"
    :cryptography "Kryptografi"
    :error-logging "Feillogging"
    :data-protection "Databeskyttelse"
    :communications "Kommunikasjon"
    :malicious "Skadelig"
    :business-logic "Forretningslogikk"
    :files-resources "Filer & ressurser"
    :api "API"
    :config "Konfigurasjon"}


   :nl
   {:main-title "Welkom op onze ASVA-beoordelingspagina"
    :preface-1 "Onze missie is om robuuste beveiligingspraktijken te integreren in onze simulatietechnologieservices. Deze beoordeling, in lijn met "
    :preface-link-1 "OWASP's ASVA-richtlijnen"
    :preface-2 ", zorgt ervoor dat onze applicaties veilig en veerkrachtig zijn. We gebruiken bronnen zoals de "
    :preface-link-2 "OWASP Top Tien Proactieve Controles"
    :preface-3 " om onze aanpak te sturen, en cultiveren een cultuur van veiligheid en bewustzijn. Uw expertise en samenwerking in dit proces zijn essentieel om onze toewijding aan veiligheidsexcellentie te behouden."
    :search "Zoeken..."
    :copy-location "Kopieer de locatie van de anker naar uw klembord"
    :level "Niveau {1}"
    :not-applicable "Niet van toepassing"
    :completed "Voltooid"
    :architecture "Architectuur"
    :authentication "Authenticatie"
    :session-management "Sessiebeheer"
    :access-control "Toegangscontrole"
    :validation-sanitization-encoding "Validatie Sanitisatie Encoding"
    :cryptography "Cryptografie"
    :error-logging "Foutenlogboek"
    :data-protection "Gegevensbescherming"
    :communications "Communicatie"
    :malicious "Kwaadaardig"
    :business-logic "Bedrijfslogica"
    :files-resources "Bestanden & bronnen"
    :api "API"
    :config "Configuratie"}

   :pt
   {:main-title "Bem-vindo à nossa Página de Avaliação ASVA"
    :preface-1 "Nossa missão é integrar práticas de segurança robustas aos nossos serviços de tecnologia de simulação. Esta avaliação, alinhada com "
    :preface-link-1 "as diretrizes da ASVA da OWASP"
    :preface-2 ", garante que nossas aplicações sejam seguras e resilientes. Utilizamos recursos como o "
    :preface-link-2 "OWASP Top Dez Controles Proativos"
    :preface-3 " para orientar nossa abordagem, promovendo uma cultura de segurança e consciência. Sua expertise e colaboração neste processo são fundamentais para manter nosso compromisso com a excelência em segurança."
    :search "Pesquisar..."
    :copy-location "Copie a localização da âncora para sua área de transferência"
    :level "Nível {1}"
    :not-applicable "Não aplicável"
    :completed "Concluído"
    :architecture "Arquitetura"
    :authentication "Autenticação"
    :session-management "Gestão de sessões"
    :access-control "Controle de acesso"
    :validation-sanitization-encoding "Validação Sanitização Codificação"
    :cryptography "Criptografia"
    :error-logging "Registro de Erros"
    :data-protection "Proteção de Dados"
    :communications "Comunicações"
    :malicious "Malicioso"
    :business-logic "Lógica de Negócios"
    :files-resources "Arquivos & recursos"
    :api "API"
    :config "Configuração"}

   :es
   {:main-title "Bienvenido a nuestra página de evaluación ASVA"
    :preface-1 "Nuestra misión es integrar prácticas de seguridad robustas en nuestros servicios de tecnología de simulación. Esta evaluación, alineada con "
    :preface-link-1 "las pautas de ASVA de OWASP"
    :preface-2 ", asegura que nuestras aplicaciones sean seguras y resilientes. Utilizamos recursos como "
    :preface-link-2 "OWASP Top Ten Controles Proactivos"
    :preface-3 " para guiar nuestro enfoque, fomentando una cultura de seguridad y conciencia. Su experiencia y colaboración en este proceso son clave para mantener nuestro compromiso con la excelencia en seguridad."
    :search "Buscar..."
    :copy-location "Copiar la ubicación del ancla a su portapapeles"
    :level "Nivel {1}"
    :not-applicable "No aplicable"
    :completed "Completado"
    :architecture "Arquitectura"
    :authentication "Autenticación"
    :session-management "Gestión de sesiones"
    :access-control "Control de acceso"
    :validation-sanitization-encoding "Validación Saneamiento Codificación"
    :cryptography "Criptografía"
    :error-logging "Registro de errores"
    :data-protection "Protección de datos"
    :communications "Comunicaciones"
    :malicious "Malicioso"
    :business-logic "Lógica empresarial"
    :files-resources "Archivos & recursos"
    :api "API"
    :config "Configuración"}

   :ko
   {:main-title "우리 ASVA 평가 페이지에 오신 것을 환영합니다"
    :preface-1 "우리의 사명은 시뮬레이션 기술 서비스에 강력한 보안 관행을 통합하는 것입니다. 이 평가는 "
    :preface-link-1 "OWASP의 ASVA 가이드라인"
    :preface-2 "에 부합하여, 우리의 애플리케이션이 안전하고 복원력이 있도록 보장합니다. 우리는 "
    :preface-link-2 "OWASP 상위 십 개의 적극적인 제어"
    :preface-3 "와 같은 자원을 활용하여 접근 방식을 안내하고, 보안과 인식의 문화를 조성합니다. 이 과정에서의 귀하의 전문 지식과 협력은 우리의 보안 우수성에 대한 약속을 유지하는 데 중요합니다."
    :search "검색..."
    :copy-location "앵커의 위치를 클립보드에 복사"
    :level "수준 {1}"
    :not-applicable "해당 없음"
    :completed "완료됨"
    :architecture "건축"
    :authentication "인증"
    :session-management "세션 관리"
    :access-control "접근 제어"
    :validation-sanitization-encoding "검증 살균 인코딩"
    :cryptography "암호화"
    :error-logging "오류 로깅"
    :data-protection "데이터 보호"
    :communications "통신"
    :malicious "악의적인"
    :business-logic "비즈니스 로직"
    :files-resources "파일 & 자원"
    :api "API"
    :config "구성"}

   :tongue/fallback :en})

(def ^:private translate
  (tongue/build-translate dicts))

(defn t [& args]
  (let [[dialect country-code] (-> js/navigator .-language (str/split "-"))]
    (apply translate (-> dialect str/lower-case keyword) args)))