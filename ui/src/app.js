import React from 'react'
import { BrowserRouter as Router } from 'react-router-dom'
import { UserProvider } from './usercontext';
import Routing from './routing';

import './style/style.less';

const App = () => {
    return (
        <UserProvider>
            <Router>
                <Routing />
            </Router>
        </UserProvider>
    );
};

export default App;
