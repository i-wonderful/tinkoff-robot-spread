export default {
    name: 'Robot',
    setup() {
        const title = 'Robot'
        return { title }
    },
    data() {
        return {
            eventBus: {},
            isRun: false,
            exchanges: [],
            logs: [],
            logOrders: [],
            errors: [],
            currentRunTickers: ""
        }
    },
    methods: {
        onStart() {
            axios.get("/strategy/start")
                .then(response => {
                    console.log(response);
                    this.isRun = true;
                    let myToasteur = new Toasteur("top-right", 2000);
                    myToasteur.success('Запущен', 'Успешно', () => {
                        // do something when clicked
                    })
                })
                .catch(e => {
                    let myToasteur = new Toasteur("top-right", 2000);
                    myToasteur.error(e,'Ошибка');
                    console.info(e);
                });
        },
        onStop() {
            axios.get("/strategy/stop")
                .then(response => {
                    console.log(response);
                    this.isRun = false;
                    let myToasteur = new Toasteur("top-right", 2000);
                    myToasteur.success('Остановлен', 'Успешно');
                })
                .catch(e => {
                    console.log(e);
                    this.isRun = false;
                    new Toasteur("top-right", 2000).error( e,'Ошибка');
                });
        },
        onCancelAllOrders() {
            axios.get("/account/cancel-all-orders")
                .then(resp => {
                    this.getCurrentOrders();
                    new Toasteur("top-right", 2000).info('Заявки отменены', '');
                });

        },
        getCurrentOrders() {
            this.logOrders = [];
            axios.get("/account/orders")
                .then(response => {
                    var orders = response.data;
                    for (const order of orders) {
                        this.logOrders.push(order);
                    }
                })
        },
        getIsRun() {
            axios.get("/strategy/isrun")
                .then(response => {
                    this.isRun = response.data;
                })
        },

    },
    mounted() {
        this.eventBus = new EventBus('/eventbus');
        this.eventBus.onopen = () => {
            this.eventBus.registerHandler('LOG', (error, message) => {
                this.logs.push(message.body);
                // document.getElementById( 'log-panel' ).scrollIntoView();
            });
            this.eventBus.registerHandler('LOG_ORDER', (error, message) => {
                const order = message.body;
                const orderStatus = order.orderStatus;
                const orderIdNew = order.orderId;
                if (orderStatus == "NEW") {
                    this.logOrders.push(message.body);
                } else if (orderStatus == "CANCEL") {
                    const orderFind = this.logOrders.find(order => {
                        if (order.orderId === orderIdNew) {
                            return order;
                        }
                    });
                    const index = this.logOrders.indexOf(orderFind);
                    this.logOrders.splice(index, 1);
                } else if(orderStatus == "DONE") {
                    const orderFind = this.logOrders.find(order => {
                        if (order.orderId === orderIdNew) {
                            return order;
                        }
                    });
                    const index = this.logOrders.indexOf(orderFind);
                    this.logOrders.splice(index, 1);
                    new Toasteur("top-right", 2000).info('Заявка исполнена', 'orderId=' + order.orderId);
                }
                else {
                    console.warn("Not found orderStatus");
                }
            });
            this.eventBus.registerHandler('LOG_ERROR', (error, message) => {
                this.errors.push(message.body);
            });
            this.eventBus.registerHandler('LOG_CURRENT_RUN_TICKERS', (error, message) => {
                this.currentRunTickers = message.body;
            });
        }

        axios.get("/account/exchanges")
            .then(response => {
                this.exchanges = response.data;
            });

        this.getCurrentOrders();
        this.getIsRun();
    },

    template: `
    <div>
        <div class="row">
            <div class="col-5">
                <div v-for="(exc, index) in exchanges" >
                    <b style="padding-right: 10px">{{exc.name}}</b> 
                    <span v-if="exc.open">открыта</span>
                    <span v-else>
                        закрыта
                        <span v-if="exc.tradingDay==false">, выходной</span>
                        <span>
                            , откроется через {{exc.hoursBeforeOpen}} часов {{exc.minutesBeforeOpen}} минут
                        </span>
                    </span>
                    <br/>
                </div>
                <b>Робот запущен: </b> <span>{{isRun}}</span>
            </div>
            <div class="col-7">
                <div class="log-errors-panel">
                    <div v-for="(error, index) in errors"  >
                        {{errors[index]}}
                    </div>
                </div>
            </div>
        </div>
    
        <div>
            <button @click="onStart">Start</button>
            <button @click="onStop">Stop</button>
            <button @click="onCancelAllOrders">Отменить все заявки</button>
        </div>
        
        <h5>Заявки</h5>
        <div>
            <table class="table">
                <thead>
                    <tr>
                        <th>Тикер</th>
                        <th>Статус</th>
                        <th>Тип</th>
                        <th>Цена</th>
                        <th>orderId</th>
                    </tr>
                </thead>
                <tbody>       
                    <tr v-for="(order, index) in logOrders" >
                        <td>{{order.ticker}}</td>
                        <td>{{order.status}}</td>
                        <td>{{order.direction}}</td>
                        <td>{{order.initialPrice}} {{order.currency}}</td>
                        <td>{{order.orderId}}</td>
                    </tr>
                </tbody>
            </table>
        </div>
        <br/>
        <div>
            <span>Отслеживаемые акции:</span><span>{{currentRunTickers}}</span>
        </div>
        <h5>Основной лог</h5>
        <div id="log-panel" class="log-panel">
            <div v-for="(log, index) in logs" >
                {{logs[index]}}
            </div>
        </div>
    </div>
    `
}