export default {
    name: 'Robot',
    setup() {
        const title = 'Robot'
        return {title}
    },
    data() {
        return {
            eventBus: {},
            isRun: false,
            exchanges: [],
            logs: [],
            logOrders: [],
            errors: []
        }
    },
    methods: {
        onStart() {
            axios.get("/strategy/start")
                .then(response => {
                    console.log(response);
                    this.isRun = true;
                })
                .catch(e => {
                    console.info(e);
                });
        },
        onStop() {
            axios.get("/strategy/stop")
                .then(response => {
                    console.log(response);
                    this.isRun = false;
                })
                .catch(e => {
                    console.info(e);
                });
        },
        onCancelAllOrders() {
            axios.get("/account/cancel-all-orders")
                .then(resp => {
                    this.getCurrentOrders();
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
                const uiAction = order.uiAction;
                const orderIdNew = order.orderId;
                if (uiAction == "ADD") {
                    this.logOrders.push(message.body);
                } else if (uiAction == "REMOVE") {
                    const orderFind = this.logOrders.find(order => {
                        if (order.orderId === orderIdNew) {
                            return order;
                        }
                    });
                    const index = this.logOrders.indexOf(orderFind);
                    this.logOrders.splice(index, 1);
                } else {
                    console.warn("Not found uiAction");
                }
            });
            this.eventBus.registerHandler('LOG_ERROR', (error, message) => {
                this.errors.push(message.body);
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
            <div class="col-3">Биржи:</div>
            <div class="col-9">
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
        
        <h5>Основной лог</h5>
        <div id="log-panel" class="log-panel">
            <div v-for="(log, index) in logs" >
                {{logs[index]}}
            </div>
        </div>
        
        <br/>
        
        <h5>Лог ошибок</h5>
        <div class="log-errors-panel">
            <div v-for="(error, index) in errors"  >
                {{errors[index]}}
            </div>
        </div>
        
    </div>
      
    `
}